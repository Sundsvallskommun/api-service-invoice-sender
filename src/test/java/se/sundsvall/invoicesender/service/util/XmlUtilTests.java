package se.sundsvall.invoicesender.service.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xmlunit.builder.DiffBuilder;
import se.sundsvall.dept44.test.annotation.resource.Load;
import se.sundsvall.dept44.test.extension.ResourceLoaderExtension;

@ExtendWith(ResourceLoaderExtension.class)
class XmlUtilTests {

	@Test
	void testRemove(@Load("/files/ArchiveIndex.xml") String originalXml, @Load("/files/ArchiveIndex_removedFiles_noXmlDeclaration.xml") String wantedXml) {
		var xpathExpressionFile1 = "//file[filename='Faktura_00000001_to_9001011234.pdf']";
		var xpathExpressionFile2 = "//file[filename='Faktura_00000004_to_9301011237.pdf']";

		// Remove two <file> elements from the original XML
		var first = XmlUtil.remove(originalXml, xpathExpressionFile1);
		var resultingXml = XmlUtil.remove(first, xpathExpressionFile2);

		var diff = DiffBuilder
			.compare(resultingXml)
			.withTest(wantedXml)
			.ignoreWhitespace()
			.checkForSimilar()
			.build();

		if (diff.hasDifferences()) {
			diff.getDifferences().forEach(System.err::println);
			fail("XMLs are not similar");
		}
	}

	@Test
	void testFind(@Load("/files/ArchiveIndex.xml") final String originalXml) {
		var xpathExpression = "//file[filename='Faktura_00000001_to_9001011234.pdf']";

		var result = XmlUtil.find(originalXml, xpathExpression);

		assertThat(result).isNotNull().isNotEmpty();
		assertThat(result.select("filename").text()).isEqualTo("Faktura_00000001_to_9001011234.pdf");
		assertThat(result.select("Reminder").text()).isEqualTo("0");
		assertThat(result.select("CustomerNumber").text()).isEqualTo("9001011234");
		assertThat(result.select("SocSec").text()).isEmpty();
		assertThat(result.select("InvoiceType").text()).isEqualTo("00");
		assertThat(result.select("InvoiceNo").text()).isEqualTo("123");
		assertThat(result.select("InvoiceDate").text()).isEqualTo("2024-02-02");
		assertThat(result.select("DueDate").text()).isEqualTo("2024-03-03");
		assertThat(result.select("AGF").text()).isEqualTo("00");
		assertThat(result.select("PaymentReference").text()).isEqualTo("9001011234");
		assertThat(result.select("TotalAmount").text()).isEqualTo("1000.00");
		assertThat(result.select("Postage").text()).isEqualTo("B");
		assertThat(result.select("BuyerParty_Name").text()).isEqualTo("John Doe");
		assertThat(result.select("BuyerParty_CareOf").text()).isEqualTo("Testtorget");
		assertThat(result.select("BuyerParty_Street").text()).isEmpty();
		assertThat(result.select("BuyerParty_ZipCode").text()).isEqualTo("123 45");
		assertThat(result.select("BuyerParty_City").text()).isEqualTo("Testvall");
		assertThat(result.select("BuyerParty_Country").text()).isEmpty();
		assertThat(result.select("FUI_name").text()).isEmpty();
		assertThat(result.select("PaymentType").text()).isEmpty();
		assertThat(result.select("PaymentNo").text()).isEqualTo("1234-1234");
		assertThat(result.select("Currency").text()).isEqualTo("SEK");
	}
}
