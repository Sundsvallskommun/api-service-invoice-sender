package se.sundsvall.invoicesender.util;

import java.util.regex.Pattern;

public final class Constants {

	private Constants() {}

	public static final String X_PATH_FILENAME_EXPRESSION = "//file[filename='%s']";
	public static final Pattern RECIPIENT_PATTERN = Pattern.compile("\\w+_\\d+_to_(\\d+)\\.pdf$");
	public static final String BATCH_FILE_SUFFIX = ".zip.7z";
	public static final String DISABLED_CRON = "-";

}
