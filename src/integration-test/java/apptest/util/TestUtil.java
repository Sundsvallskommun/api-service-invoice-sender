package apptest.util;

import jcifs.smb.SmbFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class TestUtil {

	private TestUtil() {}

	public static Map<String, byte[]> extractZipFile(final SmbFile file) throws IOException {
		var decompressedData = decompress7zFile(file);

		var entries = new HashMap<String, byte[]>();
		try (var byteArrayInputStream = new ByteArrayInputStream(decompressedData);
			var zipArchiveInputStream = new ZipArchiveInputStream(byteArrayInputStream)) {
			var zipEntry = zipArchiveInputStream.getNextEntry();
			while (zipEntry != null) {
				entries.put(zipEntry.getName(), extractZipEntryData(zipArchiveInputStream));

				zipEntry = zipArchiveInputStream.getNextEntry();
			}
		}

		return entries;
	}

	public static Map<String, byte[]> extractZipFile(final File file) throws IOException {
		var decompressedData = decompress7zFile(file);

		var entries = new HashMap<String, byte[]>();
		try (var byteArrayInputStream = new ByteArrayInputStream(decompressedData);
			var zipArchiveInputStream = new ZipArchiveInputStream(byteArrayInputStream)) {
			var zipEntry = zipArchiveInputStream.getNextEntry();
			while (zipEntry != null) {
				entries.put(zipEntry.getName(), extractZipEntryData(zipArchiveInputStream));

				zipEntry = zipArchiveInputStream.getNextEntry();
			}
		}

		return entries;
	}

	private static byte[] decompress7zFile(final SmbFile file) throws IOException {
		var out = new ByteArrayOutputStream();
		try (var lzmaInputStream = new LZMACompressorInputStream(file.getInputStream())) {
			IOUtils.copy(lzmaInputStream, out);
		}
		return out.toByteArray();
	}

	private static byte[] decompress7zFile(final File file) throws IOException {
		var out = new ByteArrayOutputStream();
		try (var outFileInputStream = new FileInputStream(file);
			var lzmaInputStream = new LZMACompressorInputStream(outFileInputStream)) {
			IOUtils.copy(lzmaInputStream, out);
		}
		return out.toByteArray();
	}

	private static byte[] extractZipEntryData(final ZipArchiveInputStream in) throws IOException {
		try (var out = new ByteArrayOutputStream()) {
			IOUtils.copy(in, out);

			return out.toByteArray();
		}
	}

}
