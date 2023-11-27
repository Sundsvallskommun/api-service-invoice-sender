package se.sundsvall.invoicesender;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;

public class Test {

    public static void main(String[] args) throws Exception {
        try (var fileInputStream = new FileInputStream("/tmp/test.zip.7z"); var lzmaInputStream = new LZMACompressorInputStream(fileInputStream)) {
            Files.copy(lzmaInputStream, Paths.get("/tmp/test.zip"), StandardCopyOption.REPLACE_EXISTING);
        }

        /*
        try (var fileInputStream = new FileOutputStream("/tmp/NEW.test.zip.7z"); var lzmaOutputStream = new LZMACompressorOutputStream(fileInputStream)) {
            Files.copy(Paths.get("/tmp/test.zip"), lzmaOutputStream);
        }*/
    }
}
