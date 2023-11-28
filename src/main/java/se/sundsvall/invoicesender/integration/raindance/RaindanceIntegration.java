package se.sundsvall.invoicesender.integration.raindance;

import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static se.sundsvall.invoicesender.model.Status.SENT;

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
import jcifs.context.SingletonContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;

@Component
@EnableConfigurationProperties(RaindanceIntegrationProperties.class)
public class RaindanceIntegration {

    private static final Logger LOG = LoggerFactory.getLogger(RaindanceIntegration.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

    private static final Predicate<Item> UNSENT_ITEMS = item -> item.getStatus() != SENT;

    private final Path workDirectory;
    private final List<String> filenamePrefixes;
    private final CIFSContext context;
    private final String shareUrl;

    RaindanceIntegration(final RaindanceIntegrationProperties properties) throws IOException {
        workDirectory = Paths.get(properties.local().workDirectory());
        if (!Files.exists(workDirectory)) {
            Files.createDirectories(workDirectory);
        }

        this.filenamePrefixes = properties.filenamePrefixes();

        // Initialize the JCIFS context
        SingletonContext.init(properties.jcifsProperties());
        context = SingletonContext.getInstance()
            .withCredentials(new NtlmPasswordAuthenticator(
                properties.domain(), properties.username(), properties.password()));
        shareUrl = String.format("smb://%s/%s", properties.host(), properties.share());
    }

    public List<Batch> readBatch(final LocalDate date) throws IOException {
        LOG.info("Reading batch for {}", date);

        // Use a random sub-work-directory
        var currentWorkDirectory = workDirectory.resolve(UUID.randomUUID().toString());
        // Create it
        Files.createDirectories(currentWorkDirectory);

        try (var share = new SmbFile(shareUrl, context)) {
            var batches = new ArrayList<Batch>();
            for (var file : share.listFiles((dir, name) ->
                    // Does the filename start with any of the configured prefixes?
                    filenamePrefixes.stream().anyMatch(name::startsWith) &&
                    // Does the filename contain the date?
                    name.contains(date.format(DATE_FORMATTER)) &&
                    // Does the filename end with ".zip.7z" ?
                    name.toLowerCase().endsWith(".zip.7z"))) {
                // Extract the filename from  the remote canonical UNC path
                var filename = file.getCanonicalUncPath();
                filename = filename.substring(filename.lastIndexOf('/') + 1);

                // Create a batch
                var batch = new Batch()
                    .withPath(currentWorkDirectory.toString())
                    .withBasename(filename.replaceAll("\\.zip\\.7z$", ""))
                    .withRemotePath(file.getCanonicalUncPath());

                LOG.info("Processing 7z file '{}'", filename);

                // Store the 7z file locally
                var sevenZipFile = currentWorkDirectory.resolve(filename).toFile();
                try (var out = new FileOutputStream(sevenZipFile)) {
                    IOUtils.copy(file.getInputStream(), out);
                }

                // Decompress the 7z (LZMA) file to a single ZIP file
                var zipFilename = filename.replaceAll("\\.7z$", "");
                var zipFile = currentWorkDirectory.resolve(zipFilename);
                try (var fileInputStream = new FileInputStream(sevenZipFile);
                     var lzmaInputStream = new LZMACompressorInputStream(fileInputStream)) {
                    Files.copy(lzmaInputStream, zipFile, StandardCopyOption.REPLACE_EXISTING);
                }

                // Process the ZIP file
                try (var zipFileInputStream = new FileInputStream(zipFile.toFile());
                     var zipArchiveInputStream = new ZipArchiveInputStream(zipFileInputStream)) {

                    var zipEntry = zipArchiveInputStream.getNextZipEntry();
                    while (zipEntry != null) {
                        LOG.info("  Found file '{}'", zipEntry.getName());

                        // Store the file locally
                        var zipEntryOutFile = currentWorkDirectory.resolve(zipEntry.getName()).toFile();
                        try (var zipEntryOutputStream = new FileOutputStream(zipEntryOutFile)) {
                            IOUtils.copy(zipArchiveInputStream, zipEntryOutputStream);
                        }

                        // Add the item to the current batch
                        batch.addItem(new Item(zipEntry.getName()));
                        zipEntry = zipArchiveInputStream.getNextZipEntry();
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
        var batchSevenZipPath = batchPath.resolve(batch.getBasename().concat(".zip.7z"));

        recreateSevenZipFile(batch);

        try (var file = new SmbFile(batch.getRemotePath() + ".NEW", context)) {
            LOG.info("Storing file '{}'", batch.getRemotePath() + ".NEW");

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
        var batchSevenZipFilePath = batchPath.resolve(batch.getBasename().concat(".zip.7z"));
        LOG.info("Creating 7z file '{}'", batchSevenZipFilePath.getFileName());
        try (var fileInputStream = new FileOutputStream(batchSevenZipFilePath.toFile());
             var lzmaOutputStream = new LZMACompressorOutputStream(fileInputStream)) {
            IOUtils.copy(batchZipFilePath.toFile(), lzmaOutputStream);
        }
    }
}
