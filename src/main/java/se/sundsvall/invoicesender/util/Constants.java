package se.sundsvall.invoicesender.util;

import static java.util.function.Predicate.not;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.IGNORED;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.METADATA_INCOMPLETE;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.NOT_SENT;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.RECIPIENT_PARTY_ID_NOT_FOUND;
import static se.sundsvall.invoicesender.integration.db.entity.ItemType.INVOICE;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import se.sundsvall.invoicesender.integration.db.entity.ItemEntity;

public final class Constants {

	private Constants() {

	}

	public static final Predicate<ItemEntity> ITEM_IS_A_PDF = item -> item.getFilename().toLowerCase().endsWith(".pdf");
	public static final Predicate<ItemEntity> ITEM_IS_AN_INVOICE = ITEM_IS_A_PDF.and(item -> item.getType() == INVOICE);
	public static final Predicate<ItemEntity> ITEM_IS_IGNORED = item -> item.getStatus() == IGNORED;
	public static final Predicate<ItemEntity> ITEM_LACKS_METADATA = item -> item.getStatus() == METADATA_INCOMPLETE;
	public static final Predicate<ItemEntity> ITEM_IS_NOT_PROCESSABLE = not(ITEM_IS_AN_INVOICE).or(ITEM_IS_IGNORED).or(ITEM_LACKS_METADATA);
	public static final Predicate<ItemEntity> RECIPIENT_HAS_INVALID_LEGAL_ID = item -> item.getStatus() == RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID;
	public static final Predicate<ItemEntity> RECIPIENT_HAS_INVALID_PARTY_ID = item -> item.getStatus() == RECIPIENT_PARTY_ID_NOT_FOUND;
	public static final Predicate<ItemEntity> INVOICE_COULD_NOT_BE_SENT = item -> item.getStatus() == NOT_SENT;

	public static final String X_PATH_FILENAME_EXPRESSION = "//file[filename='%s']";
	public static final Pattern RECIPIENT_PATTERN = Pattern.compile("\\w+_\\d+_to_(\\d+)\\.pdf$");
	public static final String BATCH_FILE_SUFFIX = ".zip.7z";
	public static final String DISABLED_CRON = "-";

}
