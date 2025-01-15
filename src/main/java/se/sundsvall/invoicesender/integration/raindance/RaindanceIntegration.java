package se.sundsvall.invoicesender.integration.raindance;

import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.UNHANDLED;
import static se.sundsvall.invoicesender.integration.db.entity.ItemType.UNKNOWN;
import static se.sundsvall.invoicesender.service.model.ItemPredicate.UNSENT_ITEMS;
import static se.sundsvall.invoicesender.util.Constants.BATCH_FILE_SUFFIX;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.Deflater;
import jcifs.CIFSContext;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sundsvall.invoicesender.integration.db.entity.BatchEntity;
import se.sundsvall.invoicesender.integration.db.entity.ItemEntity;

public class RaindanceIntegration {

	private static final Logger LOG = LoggerFactory.getLogger(RaindanceIntegration.class);

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

	private final String host;
	private final int port;

	private final FileSystem fileSystem;
	private final Path localWorkDirectory;
	private final Map<String, RaindanceIntegrationProperties.RaindanceEnvironment.BatchSetup> batchSetup;
	private final String outputFileExtraSuffix;
	private final CIFSContext context;
	private final String incomingShareUrl;

	public RaindanceIntegration(final RaindanceIntegrationProperties.RaindanceEnvironment environment, final FileSystem fileSystem) {
		this.fileSystem = fileSystem;

		try {
			host = environment.host();
			port = environment.port();

			localWorkDirectory = fileSystem.getPath(environment.localWorkDirectory());
			if (!Files.exists(localWorkDirectory)) {
				Files.createDirectories(localWorkDirectory);
			}

			batchSetup = environment.batchSetup();
			outputFileExtraSuffix = environment.outputFileExtraSuffix();

			// Initialize the JCIFS context
			var config = new PropertyConfiguration(environment.jcifsProperties());

			context = new BaseContext(config)
				.withCredentials(new NtlmPasswordAuthenticator(
					environment.domain(), environment.username(), environment.password()));

			incomingShareUrl = String.format("smb://%s:%d/%s", host, port, appendTrailingSlashIfMissing(environment.share()));

			LOG.info("Raindance will be reading from {}", incomingShareUrl);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to initialize Raindance integration", e);
		}
	}

	public List<BatchEntity> readBatches(final LocalDate date, final String batchName, final String municipalityId) throws IOException {
		LOG.info("Reading batch(es) for {}", date);

		var datePart = date.format(DATE_FORMATTER);
		try (var share = new SmbFile(incomingShareUrl, context)) {
			var batches = new ArrayList<BatchEntity>();

			for (var file : share.listFiles()) {
				var filename = file.getName();

				// Filter manually - match on batch name

				// filename starts with batchName parameter
				// filename contains "-" + datePart + "_"
				// filename lower-case ends with BATCH_FILE_SUFFIX

				// if not - skip file, close and continue as below

				if (!filename.startsWith(batchName) || !filename.contains("-" + datePart + "_") || !filename.toLowerCase().endsWith(BATCH_FILE_SUFFIX)) {
					LOG.info("Skipping file '{}'", filename);

					file.close();

					continue;
				}

				// Get the matching batch setup
				var matchingBatchSetup = batchSetup.get(batchName);

				// Use a random sub-work-directory for the batch
				var localBatchWorkDirectory = localWorkDirectory.resolve(UUID.randomUUID().toString());
				Files.createDirectories(localBatchWorkDirectory);

				// Create a batch
				var batchEntity = new BatchEntity()
					.withDate(date)
					.withMunicipalityId(municipalityId)
					.withLocalPath(localBatchWorkDirectory.toString())
					.withBasename(filename.replaceAll("\\.zip\\.7z$", ""))
					.withTargetPath(matchingBatchSetup.targetPath())
					.withArchivePath(matchingBatchSetup.archivePath())
					.withProcessingEnabled(matchingBatchSetup.process());
				// Read/copy the file data
				try (var in = file.getInputStream(); var baos = new ByteArrayOutputStream()) {
					IOUtils.copy(in, baos);
					batchEntity.setData(baos.toByteArray());
				}
				file.close();

				// Store the 7z file locally
				var sevenZipFile = localBatchWorkDirectory.resolve(filename);
				try (var out = Files.newOutputStream(sevenZipFile)) {
					IOUtils.copy(new ByteArrayInputStream(batchEntity.getData()), out);
				}

				LOG.info("Processing 7z file '{}' using work directory '{}'", filename, localBatchWorkDirectory.toAbsolutePath());

				// Decompress the 7z (LZMA) file to a single ZIP file
				var zipFilename = filename.replaceAll("\\.7z$", "");
				var zipFile = localBatchWorkDirectory.resolve(zipFilename);
				try (var fileInputStream = Files.newInputStream(sevenZipFile);
					var lzmaInputStream = new LZMACompressorInputStream(fileInputStream)) {
					Files.copy(lzmaInputStream, zipFile, StandardCopyOption.REPLACE_EXISTING);
				}

				// Process the ZIP file
				try (var zipFileInputStream = Files.newInputStream(zipFile);
					var zipArchiveInputStream = new ZipArchiveInputStream(zipFileInputStream)) {

					var zipEntry = zipArchiveInputStream.getNextEntry();
					while (zipEntry != null) {
						var zipEntryName = zipEntry.getName();

						// Mitigate potential "zip-slip"
						if (localBatchWorkDirectory.resolve(zipEntryName).normalize().startsWith("..")) {
							LOG.info("  Skipping file '{}'", zipEntryName);

							continue;
						}

						LOG.info("  Found file '{}'", zipEntryName);

						// Store the file locally
						var zipEntryOutFile = localBatchWorkDirectory.resolve(zipEntryName);
						try (var zipEntryOutputStream = Files.newOutputStream(zipEntryOutFile)) {
							IOUtils.copy(zipArchiveInputStream, zipEntryOutputStream);
						}

						// Add the item to the current batch
						batchEntity.getItems().add(new ItemEntity()
							.withFilename(zipEntryName)
							.withStatus(UNHANDLED)
							.withType(UNKNOWN));

						zipEntry = zipArchiveInputStream.getNextEntry();
					}
				}
				batchEntity.setTotalItems(batchEntity.getItems().size());
				batches.add(batchEntity);
			}

			LOG.info("Read {} batch(es)", batches.size());

			return batches;
		}
	}

	public void writeBatch(final BatchEntity batch) throws IOException {
		var targetPath = String.format("smb://%s:%d/%s%s", host, port,
			appendTrailingSlashIfMissing(batch.getTargetPath()),
			batch.getBasename() + BATCH_FILE_SUFFIX + outputFileExtraSuffix);

		LOG.info("Storing batch '{}'", targetPath);

		if (batch.isProcessingEnabled()) {
			var batchPath = fileSystem.getPath(batch.getLocalPath());
			var batchSevenZipPath = batchPath.resolve(batch.getBasename().concat(BATCH_FILE_SUFFIX));

			recreateSevenZipFile(batch);

			try (var file = new SmbFile(targetPath, context)) {
				try (var out = file.getOutputStream(); var in = Files.newInputStream(batchSevenZipPath)) {
					IOUtils.copy(in, out);
				}
			}
		} else {
			try (var file = new SmbFile(targetPath, context)) {
				try (var out = file.getOutputStream(); var in = new ByteArrayInputStream(batch.getData())) {
					IOUtils.copy(in, out);
				}
			}
		}
	}

	public void archiveOriginalBatch(final BatchEntity batch) throws IOException {
		var sourcePath = incomingShareUrl + batch.getBasename() + BATCH_FILE_SUFFIX;
		var targetPath = String.format("smb://%s:%d/%s%s", host, port,
			appendTrailingSlashIfMissing(batch.getArchivePath()),
			batch.getBasename() + BATCH_FILE_SUFFIX + outputFileExtraSuffix);

		try (var archiveFile = new SmbFile(targetPath, context)) {
			LOG.info("Archiving batch '{}' to '{}", sourcePath, targetPath);

			try (var out = archiveFile.getOutputStream(); var in = new ByteArrayInputStream(batch.getData())) {
				IOUtils.copy(in, out);
			}
		}

		try (var sourceFile = new SmbFile(sourcePath, context)) {
			sourceFile.delete();
		} catch (Exception e) {
			LOG.warn("Unable to delete source file: {}", e.getMessage());
		}
	}

	private void recreateSevenZipFile(final BatchEntity batch) throws IOException {
		var batchPath = fileSystem.getPath(batch.getLocalPath());

		// We're only interested in putting back unsent items
		var unsentItems = batch.getItems().stream()
			.filter(UNSENT_ITEMS)
			.toList();

		// Create the ZIP file
		var batchZipFilePath = batchPath.resolve(batch.getBasename().concat(".zip"));
		LOG.info("Creating ZIP file '{}'", batchZipFilePath.getFileName());
		try (var zipOutputStream = new ZipArchiveOutputStream(batchZipFilePath, WRITE, TRUNCATE_EXISTING)) {
			zipOutputStream.setLevel(Deflater.BEST_COMPRESSION);

			for (var item : unsentItems) {
				LOG.info(" Adding file '{}'", item.getFilename());

				var itemPath = batchPath.resolve(item.getFilename());
				var itemEntry = new ZipArchiveEntry(itemPath, item.getFilename());

				zipOutputStream.putArchiveEntry(itemEntry);
				try (var itemFileInputStream = Files.newInputStream(itemPath)) {
					IOUtils.copy(itemFileInputStream, zipOutputStream);
				}
				zipOutputStream.closeArchiveEntry();
			}
		}

		// Compress the ZIP file to a 7z (LZMA) file
		var batchSevenZipFilePath = batchPath.resolve(batch.getBasename().concat(BATCH_FILE_SUFFIX));
		LOG.info("Creating 7z file '{}'", batchSevenZipFilePath.getFileName());
		try (var zipFileInputStream = Files.newInputStream(batchZipFilePath);
			var sevenZipFileOutputStream = Files.newOutputStream(batchSevenZipFilePath);
			var lzmaOutputStream = new LZMACompressorOutputStream(sevenZipFileOutputStream)) {
			IOUtils.copy(zipFileInputStream, lzmaOutputStream);
		}
	}

	String appendTrailingSlashIfMissing(final String string) {
		return string.endsWith("/") ? string : string + "/";
	}

	public Set<String> getBatchSetups() {
		return this.batchSetup.keySet();
	}
}
