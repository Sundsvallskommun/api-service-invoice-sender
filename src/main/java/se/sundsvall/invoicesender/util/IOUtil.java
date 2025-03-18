package se.sundsvall.invoicesender.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream;
import org.apache.commons.io.IOUtils;

public final class IOUtil {

	private IOUtil() {}

	public static byte[] readFile(final InputStream in) throws IOException {
		try (var byteArrayOutputStream = new ByteArrayOutputStream()) {
			copy(in, byteArrayOutputStream);

			return byteArrayOutputStream.toByteArray();
		}
	}

	public static byte[] decompressLzma(final byte[] data) throws IOException {
		try (var byteArrayInputStream = new ByteArrayInputStream(data)) {
			return decompressLzma(byteArrayInputStream);
		}
	}

	public static byte[] decompressLzma(final InputStream in) throws IOException {
		try (var lzmaInputStream = new LZMACompressorInputStream(in);
			var byteArrayOutputStream = new ByteArrayOutputStream()) {

			copy(lzmaInputStream, byteArrayOutputStream);

			return byteArrayOutputStream.toByteArray();
		}
	}

	public static byte[] compressLzma(final InputStream in) throws IOException {
		try (var byteArrayOutputStream = new ByteArrayOutputStream();
			var lzmaOutputStream = new LZMACompressorOutputStream(byteArrayOutputStream)) {

			copy(in, lzmaOutputStream);
			lzmaOutputStream.finish();

			return byteArrayOutputStream.toByteArray();
		}
	}

	public static Map<String, byte[]> unzip(final byte[] data) throws IOException {
		try (var byteArrayInputStream = new ByteArrayInputStream(data)) {
			return unzip(byteArrayInputStream);
		}
	}

	public static Map<String, byte[]> unzip(final InputStream in) throws IOException {
		var result = new HashMap<String, byte[]>();

		try (var zipArchiveInputStream = new ZipArchiveInputStream(in)) {
			var zipEntry = zipArchiveInputStream.getNextEntry();
			while (zipEntry != null) {
				var zipEntryName = zipEntry.getName();

				try (var baos = new ByteArrayOutputStream()) {
					copy(zipArchiveInputStream, baos);

					result.put(zipEntryName, baos.toByteArray());
				}

				zipEntry = zipArchiveInputStream.getNextEntry();
			}

			return result;
		}
	}

	public static byte[] zip(final Map<String, byte[]> entries) throws IOException {
		try (var byteArrayOutputStream = new ByteArrayOutputStream();
			var zipOutputStream = new ZipArchiveOutputStream(byteArrayOutputStream)) {
			zipOutputStream.setLevel(Deflater.BEST_COMPRESSION);

			for (var entry : entries.entrySet()) {
				zipOutputStream.putArchiveEntry(new ZipArchiveEntry(entry.getKey()));
				try (var byteArrayInputStream = new ByteArrayInputStream(entry.getValue())) {
					copy(byteArrayInputStream, zipOutputStream);
				}
				zipOutputStream.closeArchiveEntry();
			}
			zipOutputStream.finish();

			return byteArrayOutputStream.toByteArray();
		}
	}

	public static void copy(final InputStream in, final OutputStream out) throws IOException {
		IOUtils.copy(in, out);
	}
}
