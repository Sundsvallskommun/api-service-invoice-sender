package se.sundsvall.invoicesender.integration.raindance;

import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Optional.ofNullable;
import static se.sundsvall.invoicesender.model.ItemStatus.SENT;

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
import java.util.UUID;
import java.util.function.Predicate;
import java.util.zip.Deflater;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
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
    public static final String BATCH_FILE_SUFFIX = ".zip.7z";

    private final Path workDirectory;
    private final List<String> batchFilenamePrefixes;
    private final String outputFileExtraSuffix;
    private final CIFSContext context;
    private final String shareUrl;

    RaindanceIntegration(final RaindanceIntegrationProperties properties) throws IOException {
        workDirectory = Paths.get(properties.workDirectory());
        if (!Files.exists(workDirectory)) {
            Files.createDirectories(workDirectory);
        }

        batchFilenamePrefixes = ofNullable(properties.batchFilenamePrefixes()).orElse(List.of());
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
        shareUrl = String.format("smb://%s:%d/%s", properties.host(), properties.port(),
            properties.share().endsWith("/") ? properties.share() : properties.share() + "/");
    }

    public List<Batch> readBatch(final LocalDate date) throws IOException {
        LOG.info("Reading batch for {} with filename prefix(es): {}", date, batchFilenamePrefixes);

        try (var share = new SmbFile(shareUrl, context)) {
            var batches = new ArrayList<Batch>();

            for (var batchFile : share.listFiles()) {
                var datePart = date.format(DATE_FORMATTER);
                var batchFilename = batchFile.getName();

                // Filter manually
                if (batchFilenamePrefixes.stream().noneMatch(batchFilename::startsWith) ||
                        !batchFilename.contains("-" + datePart + "_") ||
                        !batchFilename.toLowerCase().endsWith(BATCH_FILE_SUFFIX)) {
                    LOG.debug("Skipping file '{}'", batchFilename);

                    continue;
                }

                // Use a random sub-work-directory for the batch
                var batchWorkDirectory = workDirectory.resolve(UUID.randomUUID().toString());
                // Create it
                Files.createDirectories(batchWorkDirectory);

                var baos = new ByteArrayOutputStream();
                IOUtils.copy(batchFile.getInputStream(), baos);

                // Create a batch
                var batch = new Batch()
                    .withPath(batchWorkDirectory.toString())
                    .withBasename(batchFilename.replaceAll("\\.zip\\.7z$", ""))
                    .withData(baos.toByteArray())
                    .withRemotePath(batchFile.getCanonicalUncPath());

                LOG.info("Processing 7z file '{}' using work directory '{}'", batchFilename, batchWorkDirectory.toAbsolutePath());

                // Store the 7z file locally
                var sevenZipFile = batchWorkDirectory.resolve(batchFilename).toFile();
                try (var out = new FileOutputStream(sevenZipFile)) {
                    IOUtils.copy(batchFile.getInputStream(), out);
                }

                // Decompress the 7z (LZMA) file to a single ZIP file
                var zipFilename = batchFilename.replaceAll("\\.7z$", "");
                var zipFile = batchWorkDirectory.resolve(zipFilename);
                try (var fileInputStream = new FileInputStream(sevenZipFile);
                     var lzmaInputStream = new LZMACompressorInputStream(fileInputStream)) {
                    Files.copy(lzmaInputStream, zipFile, StandardCopyOption.REPLACE_EXISTING);
                }

                // Process the ZIP file
                try (var zipFileInputStream = new FileInputStream(zipFile.toFile());
                     var zipArchiveInputStream = new ZipArchiveInputStream(zipFileInputStream)) {

                    var zipEntry = zipArchiveInputStream.getNextEntry();
                    while (zipEntry != null) {
                        LOG.info("  Found file '{}'", zipEntry.getName());

                        // Store the file locally
                        var zipEntryOutFile = batchWorkDirectory.resolve(zipEntry.getName()).toFile();
                        try (var zipEntryOutputStream = new FileOutputStream(zipEntryOutFile)) {
                            IOUtils.copy(zipArchiveInputStream, zipEntryOutputStream);
                        }

                        // Add the item to the current batch
                        batch.addItem(new Item(zipEntry.getName()));
                        zipEntry = zipArchiveInputStream.getNextEntry();
                    }
                }

                batches.add(batch);
            }

            LOG.info("Batch read with {} file(s)", batches.size());

            return batches;
        }
    }

    public void writeBatch(final Batch batch) throws IOException {
        var batchPath = Paths.get(batch.getPath());
        var batchSevenZipPath = batchPath.resolve(batch.getBasename().concat(BATCH_FILE_SUFFIX));

        recreateSevenZipFile(batch);

        try (var file = new SmbFile(batch.getRemotePath() + outputFileExtraSuffix, context)) {
            LOG.info("Storing file '{}'", batch.getRemotePath());

            var batchSevenZipFile = batchSevenZipPath.toFile();
            try (var in = new FileInputStream(batchSevenZipFile)) {
                IOUtils.copy(in, file.getOutputStream());
            }
        }
    }

    private void recreateSevenZipFile(final Batch batch) throws IOException {
        var batchPath = Paths.get(batch.getPath());

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
        try (var fileInputStream = new FileOutputStream(batchSevenZipFilePath.toFile());
             var lzmaOutputStream = new LZMACompressorOutputStream(fileInputStream)) {
            IOUtils.copy(batchZipFilePath.toFile(), lzmaOutputStream);
        }
    }
}
