package se.sundsvall.invoicesender.integration.raindance;

import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static se.sundsvall.invoicesender.model.ItemStatus.SENT;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.zip.Deflater;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import se.sundsvall.invoicesender.model.Batch;
import se.sundsvall.invoicesender.model.Item;

import jcifs.CIFSContext;
import jcifs.CIFSException;
import jcifs.context.SingletonContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;

@Component
@EnableConfigurationProperties(RaindanceIntegrationProperties.class)
public class RaindanceIntegration {

	private static final Logger LOG = LoggerFactory.getLogger(RaindanceIntegration.class);

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
	private static final Predicate<Item> UNSENT_ITEMS = item -> item.getStatus() != SENT;
	private static final String BATCH_FILE_SUFFIX = ".zip.7z";

	private final String host;
	private final int port;

	private final Path localWorkDirectory;
	private final Map<String, RaindanceIntegrationProperties.BatchSetup> batchFileSetup;
	private final String outputFileExtraSuffix;
	private final CIFSContext context;
	private final String incomingShareUrl;

	RaindanceIntegration(final RaindanceIntegrationProperties properties) throws IOException {
		host = properties.host();
		port = properties.port();

		localWorkDirectory = Paths.get(properties.localWorkDirectory());
		if (!Files.exists(localWorkDirectory)) {
			Files.createDirectories(localWorkDirectory);
		}

		batchFileSetup = properties.batchSetup();
		outputFileExtraSuffix = properties.outputFileExtraSuffix();

		// Attempt to initialize the JCIFS context
		try {
			LOG.info("Raindance connection timeout set to {} ms and response timeout set to {} ms",
				properties.connectTimeout().toMillis(), properties.responseTimeout().toMillis());

			SingletonContext.init(properties.jcifsProperties());
		} catch (CIFSException e) {
			LOG.info("JCIFS context already initialized");
		}

		context = SingletonContext.getInstance()
			.withCredentials(new NtlmPasswordAuthenticator(
				properties.domain(), properties.username(), properties.password()));

		incomingShareUrl = String.format("smb://%s:%d/%s", host, port, appendTrailingSlashIfMissing(properties.share()));

		LOG.info("Raindance will be reading from {}", incomingShareUrl);
	}

	public List<Batch> readBatches(final LocalDate date) throws IOException {
		LOG.info("Reading batch(es) for {}", date);

		var datePart = date.format(DATE_FORMATTER);
		try (var share = new SmbFile(incomingShareUrl, context)) {
			var batches = new ArrayList<Batch>();

			for (var file : share.listFiles()) {
				var filename = file.getName();

				// Filter manually
				var matchingBatchSetup = batchFileSetup.entrySet().stream()
					.filter(entry -> filename.startsWith(entry.getKey()) &&
						filename.contains("-" + datePart + "_") &&
						filename.toLowerCase().endsWith(BATCH_FILE_SUFFIX))
					.findFirst()
					.map(Map.Entry::getValue)
					.orElse(null);

				if (matchingBatchSetup == null) {
					LOG.info("Skipping file '{}'", filename);

					file.close();

					continue;
				}

				// Use a random sub-work-directory for the batch
				var localBatchWorkDirectory = localWorkDirectory.resolve(UUID.randomUUID().toString());
				Files.createDirectories(localBatchWorkDirectory);

				// Create a batch
				var batch = new Batch()
					.withLocalPath(localBatchWorkDirectory.toString())
					.withBasename(filename.replaceAll("\\.zip\\.7z$", ""))
					.withTargetPath(matchingBatchSetup.targetPath())
					.withArchivePath(matchingBatchSetup.archivePath())
					.withProcess(matchingBatchSetup.process());
				// Read/copy the file data
				try (var in = file.getInputStream(); var baos = new ByteArrayOutputStream()) {
					IOUtils.copy(in, baos);

					batch.setData(baos.toByteArray());
				}
				file.close();

				// Store the 7z file locally
				var sevenZipFile = localBatchWorkDirectory.resolve(filename).toFile();
				try (var out = new FileOutputStream(sevenZipFile)) {
					IOUtils.copy(new ByteArrayInputStream(batch.getData()), out);
				}

				LOG.info("Processing 7z file '{}' using work directory '{}'", filename, localBatchWorkDirectory.toAbsolutePath());

				// Decompress the 7z (LZMA) file to a single ZIP file
				var zipFilename = filename.replaceAll("\\.7z$", "");
				var zipFile = localBatchWorkDirectory.resolve(zipFilename);
				try (var fileInputStream = new FileInputStream(sevenZipFile);
					var lzmaInputStream = new LZMACompressorInputStream(fileInputStream)) {
					Files.copy(lzmaInputStream, zipFile, StandardCopyOption.REPLACE_EXISTING);
				}

				// Process the ZIP file
				try (var zipFileInputStream = new FileInputStream(zipFile.toFile());
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
						var zipEntryOutFile = localBatchWorkDirectory.resolve(zipEntryName).toFile();
						try (var zipEntryOutputStream = new FileOutputStream(zipEntryOutFile)) {
							IOUtils.copy(zipArchiveInputStream, zipEntryOutputStream);
						}

						// Add the item to the current batch
						batch.addItem(new Item(zipEntryName));
						zipEntry = zipArchiveInputStream.getNextEntry();
					}
				}

				batches.add(batch);
			}

			LOG.info("Read {} batch(es)", batches.size());

			return batches;
		}
	}

	public void writeBatch(final Batch batch) throws IOException {
		var targetPath = String.format("smb://%s:%d/%s%s", host, port,
			appendTrailingSlashIfMissing(batch.getTargetPath()),
			batch.getBasename() + RaindanceIntegration.BATCH_FILE_SUFFIX + outputFileExtraSuffix);

		LOG.info("Storing batch '{}'", targetPath);

		if (batch.isProcessingEnabled()) {
			var batchPath = Paths.get(batch.getLocalPath());
			var batchSevenZipPath = batchPath.resolve(batch.getBasename().concat(BATCH_FILE_SUFFIX));

			recreateSevenZipFile(batch);

			try (var file = new SmbFile(targetPath, context)) {
				var batchSevenZipFile = batchSevenZipPath.toFile();
				try (var out = file.getOutputStream(); var in = new FileInputStream(batchSevenZipFile)) {
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

	public void archiveOriginalBatch(final Batch batch) throws IOException {
		var sourcePath = incomingShareUrl + batch.getBasename() + BATCH_FILE_SUFFIX;
		var targetPath = String.format("smb://%s:%d/%s%s", host, port,
			appendTrailingSlashIfMissing(batch.getArchivePath()),
			batch.getBasename() + RaindanceIntegration.BATCH_FILE_SUFFIX + outputFileExtraSuffix);

		try (var archiveFile = new SmbFile(targetPath, context)) {
			LOG.info("Archiving batch '{}' to '{}", sourcePath, targetPath);

			try (var out = archiveFile.getOutputStream(); var in = new ByteArrayInputStream(batch.getData())) {
				IOUtils.copy(in, out);
			}
		}

		try (var sourceFile = new SmbFile(sourcePath, context)) {
			sourceFile.delete();
		} catch (Exception e) {
			LOG.warn("Unable to delete source file", e);
		}
	}

	private void recreateSevenZipFile(final Batch batch) throws IOException {
		var batchPath = Paths.get(batch.getLocalPath());

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
				var itemFile = itemPath.toFile();
				var itemEntry = new ZipArchiveEntry(itemPath, item.getFilename());

				zipOutputStream.putArchiveEntry(itemEntry);
				try (var itemFileInputStream = new FileInputStream(itemFile)) {
					IOUtils.copy(itemFileInputStream, zipOutputStream);
				}
				zipOutputStream.closeArchiveEntry();
			}
		}

		// Compress the ZIP file to a 7z (LZMA) file
		var batchSevenZipFilePath = batchPath.resolve(batch.getBasename().concat(BATCH_FILE_SUFFIX));
		LOG.info("Creating 7z file '{}'", batchSevenZipFilePath.getFileName());
		try (var zipFileInputStream = new FileInputStream(batchZipFilePath.toFile());
			var sevenZipFileOutputStream = new FileOutputStream(batchSevenZipFilePath.toFile());
			var lzmaOutputStream = new LZMACompressorOutputStream(sevenZipFileOutputStream)) {
			IOUtils.copy(zipFileInputStream, lzmaOutputStream);
		}
	}

	String appendTrailingSlashIfMissing(final String s) {
		return s.endsWith("/") ? s : s + "/";
	}
}
