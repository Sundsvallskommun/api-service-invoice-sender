package se.sundsvall.invoicesender.integration.db.entity;

public enum ItemStatus {
	/** Initial status */
	UNHANDLED,
	/** Indicates that an invoice should be ignored */
	IGNORED,
	/** Indicates that an invoice lacks required metadata */
	METADATA_INCOMPLETE,
	/** Indicates that a valid legal id was found in the filename */
	RECIPIENT_LEGAL_ID_FOUND,
	/** Indicates that no valid legal id was found in the filename */
	RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID,
	/** Indicates that a party id has been found */
	RECIPIENT_PARTY_ID_FOUND,
	/** Indicates that no party id could be found */
	RECIPIENT_PARTY_ID_NOT_FOUND,
	/** Indicates that the item/invoice was sent */
	SENT,
	/** Indicates that the item/invoice was not sent */
	NOT_SENT,
	/** Indicates that the item/invoice is being processed */
	IN_PROGRESS
}
