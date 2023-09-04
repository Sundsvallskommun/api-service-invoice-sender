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
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.share.DiskShare;
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

@Component
@EnableConfigurationProperties(RaindanceIntegrationProperties.class)
public class RaindanceIntegration {

    static final Logger LOG = LoggerFactory.getLogger(RaindanceIntegration.class);

    private final FileReader fileReader;
    private final FileWriter fileWriter;

    RaindanceIntegration(final RaindanceIntegrationProperties properties) throws IOException {
        var workDirectory = Paths.get(properties.local().workDirectory());
        if (!Files.exists(workDirectory)) {
            LOG.info("Creating missing local work directory {}", workDirectory);

            Files.createDirectories(workDirectory);
        } else {
            LOG.info("Using existing local work directory {}", workDirectory);
        }

        var authenticationContext = new AuthenticationContext(properties.username(),
            properties.password().toCharArray(), properties.share());

        fileReader = new FileReader(properties, authenticationContext, workDirectory);
        fileWriter = new FileWriter(properties, authenticationContext);
    }

    public Batch readBatch() throws IOException {
        return fileReader.readBatch();
    }

    public void writeBatch(final Batch batch) throws IOException {
        fileWriter.writeBatch(batch);
    }

    static class FileReader {

        private static final Set<AccessMask> ACCESS_MASK = Set.of(AccessMask.GENERIC_READ);
        private static final Set<SMB2ShareAccess> SHARE_ACCESS = Set.of(SMB2ShareAccess.FILE_SHARE_READ);
        private static final SMB2CreateDisposition DISPOSITION = SMB2CreateDisposition.FILE_OPEN;

        private final RaindanceIntegrationProperties properties;
        private final AuthenticationContext authenticationContext;
        private final Path workDirectory;

        FileReader(final RaindanceIntegrationProperties properties,
                final AuthenticationContext authenticationContext, final Path workDirectory) {
            this.properties = properties;
            this.authenticationContext = authenticationContext;
            this.workDirectory = workDirectory;
        }

        Batch readBatch() throws IOException {
            var currentWorkDirectory = workDirectory.resolve(UUID.randomUUID().toString());

            var batch = new Batch().setPath(currentWorkDirectory.toString());

            Files.createDirectories(currentWorkDirectory);

            try (var smbClient = new SMBClient()) {
                try (var connection = smbClient.connect(properties.host())) {
                    var session = connection.authenticate(authenticationContext);

                    // Connect the share
                    try (var share = (DiskShare) session.connectShare(properties.share())) {
                        // List and filter files
                        for (var fileInfo : share.list(properties.inbound().path(), "*.7z")) {
                            LOG.info("Processing 7z file '{}'", fileInfo.getFileName());

                            var path = String.format("%s\\%s", properties.inbound().path(), fileInfo.getFileName());
                            var file = share.openFile(path, ACCESS_MASK, null, SHARE_ACCESS, DISPOSITION, null);

                            // Set the invoice batch original filename
                            batch.setSevenZipFilename(fileInfo.getFileName());

                            // Store the 7z file locally
                            var sevenZipOutFile = currentWorkDirectory.resolve(fileInfo.getFileName()).toFile();
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
                                    LOG.info("Processing ZIP file '{}'", sevenZipEntry.getName());

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
                                        LOG.info("Processing file '{}'", zipEntry.getName());

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
                            }
                        }
                    }
                }
            }

            return batch;
        }
    }

    static class FileWriter {

        private static final Predicate<Item> UNSENT_ITEMS = item -> item.getStatus() != SENT;

        private static final Set<AccessMask> ACCESS_MASK = Set.of(AccessMask.GENERIC_WRITE);
        private static final Set<FileAttributes> FILE_ATTRIBUTES = Set.of(FileAttributes.FILE_ATTRIBUTE_NORMAL);
        private static final Set<SMB2ShareAccess> SHARE_ACCESS = Set.of(SMB2ShareAccess.FILE_SHARE_WRITE);
        private static final SMB2CreateDisposition DISPOSITION = SMB2CreateDisposition.FILE_OVERWRITE_IF;

        private final RaindanceIntegrationProperties properties;
        private final AuthenticationContext authenticationContext;

        FileWriter(final RaindanceIntegrationProperties properties,
                final AuthenticationContext authenticationContext) {
            this.properties = properties;
            this.authenticationContext = authenticationContext;
        }

        void writeBatch(final Batch batch) throws IOException {
            // We're only interested in putting back unsent items - group them by zip file
            var unsentItems = batch.getItems().stream()
                .filter(UNSENT_ITEMS)
                .collect(groupingBy(Item::getZipFilename));

            var batchPath = Paths.get(batch.getPath());
            var batchSevenZipPath = batchPath.resolve(batch.getSevenZipFilename());

            try (var sevenZipFile = new SevenZOutputFile(batchSevenZipPath.toFile())) {
                LOG.info("Creating 7z file '{}'", batchSevenZipPath.getFileName());

                // Use a classic for loop to avoid having to handle IOExceptions in lambdas...
                for (var itemGroup : unsentItems.entrySet()) {
                    var zipFilename = itemGroup.getKey();

                    LOG.info("Creating ZIP file '{}'", zipFilename);

                    var zipFilePath = batchPath.resolve(zipFilename);
                    try (var zipOutputStream = new ZipArchiveOutputStream(zipFilePath, WRITE, TRUNCATE_EXISTING)) {
                        for (var item : itemGroup.getValue()) {
                            var itemFilename = item.getFilename();

                            LOG.info("Adding unhandled file '{}'", itemFilename);

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

            try (var smbClient = new SMBClient()) {
                try (var connection = smbClient.connect(properties.host())) {
                    var session = connection.authenticate(authenticationContext);

                    // Connect the share
                    try (var share = (DiskShare) session.connectShare(properties.share())) {
                        var path = String.format("%s\\%s", properties.outbound().path(), batch.getSevenZipFilename());

                        LOG.info("Storing file '{}'", path);

                        var file = share.openFile(path, ACCESS_MASK, FILE_ATTRIBUTES, SHARE_ACCESS, DISPOSITION, null);

                        var batchSevenZipFile = batchSevenZipPath.toFile();

                        // For some unknown reason, this needs to be done manually, i.e. IOUtils#copy
                        // or IOUtils.copyRange doesn't do the trick
                        try (var in = new FileInputStream(batchSevenZipFile)) {
                            var buf = new byte[8192];
                            var offset = 0l;
                            var length = 0;
                            while ((length = in.read(buf)) > 0) {
                                offset = share.getFileInformation(path).getStandardInformation().getEndOfFile();
                                file.write(buf, offset, 0, length);
                            }
                            file.flush();
                            file.close();
                        }
                    }
                }
            }
        }
    }
}
