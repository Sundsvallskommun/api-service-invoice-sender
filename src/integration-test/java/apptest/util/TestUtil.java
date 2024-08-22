package apptest.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.io.IOUtils;

public final class TestUtil {

	private TestUtil() {}

	public static Map<String, byte[]> extractZipFile(final File file) throws IOException {
		final var decompressedData = decompress7zFile(file);

		final var entries = new HashMap<String, byte[]>();
		try (final var byteArrayInputStream = new ByteArrayInputStream(decompressedData);
		     final var zipArchiveInputStream = new ZipArchiveInputStream(byteArrayInputStream)) {
			var zipEntry = zipArchiveInputStream.getNextEntry();
			while (zipEntry != null) {
				entries.put(zipEntry.getName(), extractZipEntryData(zipArchiveInputStream));

				zipEntry = zipArchiveInputStream.getNextEntry();
			}
		}

		return entries;
	}

	private static byte[] decompress7zFile(final File file) throws IOException {
		final var out = new ByteArrayOutputStream();
		try (final var outFileInputStream = new FileInputStream(file);
		     final var lzmaInputStream = new LZMACompressorInputStream(outFileInputStream)) {
			IOUtils.copy(lzmaInputStream, out);
		}
		return out.toByteArray();
	}

	private static byte[] extractZipEntryData(final ZipArchiveInputStream in) throws IOException {
		try (final var out = new ByteArrayOutputStream()) {
			IOUtils.copy(in, out);

			return out.toByteArray();
		}
	}

}
