package se.sundsvall.invoicesender.integration.raindance;

import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.stream.Collectors.groupingBy;
import static se.sundsvall.invoicesender.model.Status.SENT;
import static se.sundsvall.invoicesender.util.StreamUtil.fromIterable;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
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

    private static final Predicate<Item> UNSENT_ITEMS = item -> item.getStatus() != SENT;

    private final Path workDirectory;
    private final CIFSContext context;
    private final String shareUrl;

    RaindanceIntegration(final RaindanceIntegrationProperties properties) throws IOException {
        workDirectory = Paths.get(properties.local().workDirectory());
        if (!Files.exists(workDirectory)) {
            LOG.info("Creating missing local work directory {}", workDirectory);

            Files.createDirectories(workDirectory);
        } else {
            LOG.info("Using existing local work directory {}", workDirectory);
        }

        // Initialize the JCIFS context
        SingletonContext.init(properties.jcifsProperties());
        context = SingletonContext.getInstance()
            .withCredentials(new NtlmPasswordAuthenticator(
                properties.domain(), properties.username(), properties.password()));
        shareUrl = String.format("smb://%s/%s", properties.host(), properties.share());
    }

    public List<Batch> readBatch() throws IOException {
        // Use a random sub-work-directory
        var currentWorkDirectory = workDirectory.resolve(UUID.randomUUID().toString());
        // Create it
        Files.createDirectories(currentWorkDirectory);

        try (var share = new SmbFile(shareUrl, context)) {
            LOG.info("Connected to {}", shareUrl);

            var batches = new ArrayList<Batch>();
            for (var file : share.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip.7z"))) {
                // Create a batch
                var batch = new Batch()
                    .withPath(currentWorkDirectory.toString())
                    .withRemotePath(file.getCanonicalUncPath());

                // Extract the filename from  the remote canonical UNC path
                var filename = batch.getRemotePath();
                filename = filename.substring(filename.lastIndexOf('/') + 1);

                LOG.info("Processing 7z file '{}'", filename);

                // Set the invoice batch original filename
                batch.withSevenZipFilename(filename);

                // Store the 7z file locally
                var sevenZipOutFile = currentWorkDirectory.resolve(filename).toFile();
                try (var out = new FileOutputStream(sevenZipOutFile)) {
                    IOUtils.copy(file.getInputStream(), out);
                }

                try (var sevenZFile = new SevenZFile(sevenZipOutFile)) {
                    // Filter out anything that isn't a ZIP file
                    var sevenZipEntries = fromIterable(sevenZFile.getEntries())
                        .filter(sevenZipEntry -> sevenZipEntry.getName().toLowerCase().endsWith(".zip"))
                        .toList();

                    // Use a classic for loop to avoid having to handle IOExceptions in lambdas...
                    for (var sevenZipEntry : sevenZipEntries) {
                        LOG.info(" Processing ZIP file '{}'", sevenZipEntry.getName());

                        var sevenZInputStream = sevenZFile.getInputStream(sevenZipEntry);

                        // Store the ZIP file locally
                        var sevenZipEntryOutFile = currentWorkDirectory.resolve(sevenZipEntry.getName()).toFile();
                        try (var out = new FileOutputStream(sevenZipEntryOutFile)) {
                            IOUtils.copy(sevenZInputStream, out);
                        }

                        // "Reset" the input stream
                        sevenZInputStream = sevenZFile.getInputStream(sevenZipEntry);

                        var zipFilename = sevenZipEntry.getName();

                        var zipInputStream = new ZipArchiveInputStream(sevenZInputStream);
                        var zipEntry = zipInputStream.getNextZipEntry();
                        while (zipEntry != null) {
                            LOG.info("  Found file '{}'", zipEntry.getName());

                            // Store the file locally
                            var zipEntryOutFile = currentWorkDirectory.resolve(zipEntry.getName()).toFile();
                            IOUtils.copyRange(zipInputStream, sevenZipEntry.getSize(), new FileOutputStream(zipEntryOutFile));

                            // Add the item to the current batch
                            batch.addItem(new Item()
                                .setZipFilename(zipFilename)
                                .setFilename(zipEntry.getName()));
                            zipEntry = zipInputStream.getNextZipEntry();
                        }
                    }
                    file.close();
                }

                batches.add(batch);
            }

            return batches;
        }
    }

    public void writeBatch(final Batch batch) throws IOException {
        var batchPath = Paths.get(batch.getPath());
        var batchSevenZipPath = batchPath.resolve(batch.getSevenZipFilename());

        recreateSevenZipFile(batch);

        try (var file = new SmbFile(batch.getRemotePath() + ".NEW", context)) {
            LOG.info("Connected to {}", shareUrl);
            LOG.info("Storing file '{}'", batch.getRemotePath() + ".NEW");

            var batchSevenZipFile = batchSevenZipPath.toFile();
            try (var in = new FileInputStream(batchSevenZipFile)) {
                IOUtils.copy(in, file.getOutputStream());
            }
        }
    }

    private void recreateSevenZipFile(final Batch batch) throws IOException {
        var batchPath = Paths.get(batch.getPath());
        var batchSevenZipPath = batchPath.resolve(batch.getSevenZipFilename());

        // We're only interested in putting back unsent items - group them by zip file
        var unsentItems = batch.getItems().stream()
            .filter(UNSENT_ITEMS)
            .collect(groupingBy(Item::getZipFilename));

        try (var sevenZipFile = new SevenZOutputFile(batchSevenZipPath.toFile())) {
            LOG.info("Creating 7z file '{}'", batchSevenZipPath.getFileName());

            // Intentional use of a classic for loop to avoid having to jump through hoops and/or
            // resorting to contrived exception handling in lambdas...
            for (var itemGroup : unsentItems.entrySet()) {
                var zipFilename = itemGroup.getKey();

                LOG.info(" Creating ZIP file '{}'", zipFilename);

                var zipFilePath = batchPath.resolve(zipFilename);
                try (var zipOutputStream = new ZipArchiveOutputStream(zipFilePath, WRITE, TRUNCATE_EXISTING)) {
                    for (var item : itemGroup.getValue()) {
                        var itemFilename = item.getFilename();

                        LOG.info("  Adding file '{}'", itemFilename);

                        var itemPath = batchPath.resolve(itemFilename);
                        var itemFile = itemPath.toFile();
                        var itemEntry = new ZipArchiveEntry(itemPath, itemFilename);

                        zipOutputStream.putArchiveEntry(itemEntry);
                        IOUtils.copyRange(new FileInputStream(itemFile), itemFile.length(), zipOutputStream);
                        zipOutputStream.closeArchiveEntry();
                    }
                }

                var zipFileEntry = new SevenZArchiveEntry();
                zipFileEntry.setName(zipFilename);

                sevenZipFile.putArchiveEntry(zipFileEntry);
                sevenZipFile.write(zipFilePath);
                sevenZipFile.closeArchiveEntry();
            }
        }

    }
}
