package se.sundsvall.invoicesender.integration.raindance;

import static java.util.stream.Collectors.toMap;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.UNHANDLED;
import static se.sundsvall.invoicesender.integration.db.entity.ItemType.UNKNOWN;
import static se.sundsvall.invoicesender.service.model.ItemPredicate.UNSENT_ITEMS;
import static se.sundsvall.invoicesender.util.Constants.BATCH_FILE_SUFFIX;
import static se.sundsvall.invoicesender.util.IOUtil.compressLzma;
import static se.sundsvall.invoicesender.util.IOUtil.decompressLzma;
import static se.sundsvall.invoicesender.util.IOUtil.readFile;
import static se.sundsvall.invoicesender.util.IOUtil.unzip;
import static se.sundsvall.invoicesender.util.IOUtil.zip;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jcifs.CIFSContext;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;
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

	private final Map<String, RaindanceIntegrationProperties.RaindanceEnvironment.BatchSetup> batchSetup;
	private final CIFSContext context;
	private final String incomingShareUrl;

	public RaindanceIntegration(final RaindanceIntegrationProperties.RaindanceEnvironment environment) {
		try {
			host = environment.host();
			port = environment.port();

			batchSetup = environment.batchSetup();

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

				// Create a batch
				var batchEntity = new BatchEntity()
					.withMunicipalityId(municipalityId)
					.withDate(date)
					.withData(readFile(file.getInputStream()))
					.withFilename(filename)
					.withTargetPath(matchingBatchSetup.targetPath())
					.withArchivePath(matchingBatchSetup.archivePath())
					.withProcessingEnabled(matchingBatchSetup.process());
				file.close();

				LOG.info("Processing 7z file '{}'", filename);

				// Decompress the batch 7z (LZMA) file, resulting in a single ZIP file
				var decompressedLzmaData = decompressLzma(batchEntity.getData());
				// Unzip the ZIP file and add each item to the current batch
				unzip(decompressedLzmaData).forEach((zipEntryName, zipEntryData) -> batchEntity.getItems().add(new ItemEntity()
					.withFilename(zipEntryName)
					.withData(zipEntryData)
					.withStatus(UNHANDLED)
					.withType(UNKNOWN)));

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
			batch.getFilename());

		LOG.info("Storing batch '{}'", targetPath);

		if (batch.isProcessingEnabled()) {
			// Re-create the 7z (LZMA) file
			var sevenZipFileBytes = recreateSevenZipFile(batch);
			// Write the updated file to the Samba share
			try (var file = new SmbFile(targetPath, context);
				var out = file.getOutputStream();
				var in = new ByteArrayInputStream(sevenZipFileBytes)) {
				IOUtils.copy(in, out);
			}
		} else {
			// No processing - just write the original file to the Samba share
			try (var file = new SmbFile(targetPath, context);
				var out = file.getOutputStream();
				var in = new ByteArrayInputStream(batch.getData())) {
				IOUtils.copy(in, out);
			}
		}
	}

	public void archiveOriginalBatch(final BatchEntity batch) throws IOException {
		var sourcePath = incomingShareUrl + batch.getFilename();
		var targetPath = String.format("smb://%s:%d/%s%s", host, port,
			appendTrailingSlashIfMissing(batch.getArchivePath()),
			batch.getFilename());

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

	private byte[] recreateSevenZipFile(final BatchEntity batch) throws IOException {
		// We're only interested in putting back unsent items
		var unsentItems = batch.getItems().stream()
			.filter(UNSENT_ITEMS)
			.collect(toMap(ItemEntity::getFilename, ItemEntity::getData));

		// Zip the items
		var zipFileBytes = zip(unsentItems);
		// Compress the ZIP file to a 7z (LZMA) file and return it
		return compressLzma(new ByteArrayInputStream(zipFileBytes));
	}

	String appendTrailingSlashIfMissing(final String string) {
		return string.endsWith("/") ? string : string + "/";
	}

	public Set<String> getBatchSetups() {
		return this.batchSetup.keySet();
	}
}
