package se.sundsvall.invoicesender.service.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static se.sundsvall.invoicesender.TestDataFactory.generateNode;

import java.util.stream.Stream;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Node;
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

		assertThat(first).isNotNull();
		assertThat(resultingXml).isNotNull();

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

		assertThat(result).isNotNull();

		assertThat(XmlUtil.getChildNodeText(result, "filename")).isEqualTo("Faktura_00000001_to_9001011234.pdf");
		assertThat(XmlUtil.getChildNodeText(result, "CustomerNumber")).isEqualTo("9001011234");
		assertThat(XmlUtil.getChildNodeText(result, "SocSec")).isEmpty();
		assertThat(XmlUtil.getChildNodeText(result, "InvoiceType")).isEqualTo("00");
		assertThat(XmlUtil.getChildNodeText(result, "InvoiceNo")).isEqualTo("123");
		assertThat(XmlUtil.getChildNodeText(result, "InvoiceDate")).isEqualTo("2024-02-02");
		assertThat(XmlUtil.getChildNodeText(result, "DueDate")).isEqualTo("2024-03-03");
		assertThat(XmlUtil.getChildNodeText(result, "AGF")).isEqualTo("00");
		assertThat(XmlUtil.getChildNodeText(result, "PaymentReference")).isEqualTo("9001011234");
		assertThat(XmlUtil.getChildNodeText(result, "TotalAmount")).isEqualTo("1000.00");
		assertThat(XmlUtil.getChildNodeText(result, "Postage")).isEqualTo("B");
		assertThat(XmlUtil.getChildNodeText(result, "BuyerParty_Name")).isEqualTo("John Doe");
		assertThat(XmlUtil.getChildNodeText(result, "BuyerParty_CareOf")).isEqualTo("Testtorget");
		assertThat(XmlUtil.getChildNodeText(result, "BuyerParty_Street")).isEmpty();
		assertThat(XmlUtil.getChildNodeText(result, "BuyerParty_ZipCode")).isEqualTo("123 45");
		assertThat(XmlUtil.getChildNodeText(result, "BuyerParty_City")).isEqualTo("Testvall");
		assertThat(XmlUtil.getChildNodeText(result, "BuyerParty_Country")).isEmpty();
		assertThat(XmlUtil.getChildNodeText(result, "FUI_name")).isEmpty();
		assertThat(XmlUtil.getChildNodeText(result, "PaymentType")).isEmpty();
		assertThat(XmlUtil.getChildNodeText(result, "PaymentNo")).isEqualTo("1234-1234");
		assertThat(XmlUtil.getChildNodeText(result, "Currency")).isEqualTo("SEK");
	}

	@Test
	void testFind_shouldThrowException_whenFaultyXml() {
		var faultyXml = createFaultyXml();
		assertThatExceptionOfType(XmlUtil.XmlException.class).isThrownBy(() -> XmlUtil.find(faultyXml, "//parent/child"))
			.withMessage("Unable to parse XML")
			.withCauseInstanceOf(Exception.class);
	}

	@Test
	void testRemove_shouldThrowException_whenFaultyXml() {
		var faultyXml = createFaultyXml();
		assertThatExceptionOfType(XmlUtil.XmlException.class).isThrownBy(() -> XmlUtil.remove(faultyXml, "//parent/child"))
			.withMessage("Unable to parse XML")
			.withCauseInstanceOf(Exception.class);
	}

	@ParameterizedTest
	@MethodSource("getChildNodeTextProvider")
	void testGetChildNodeTextValidations(Node node, String childNodeName) {
		assertThat(XmlUtil.getChildNodeText(node, childNodeName)).isEmpty();
	}

	static Stream<Arguments> getChildNodeTextProvider() throws ParserConfigurationException {
		var node = generateNode();
		return Stream.of(
			Arguments.of(null, "childNodeName"),
			Arguments.of(node, null),
			Arguments.of(node, ""),
			Arguments.of(node, " "));
	}

	@ParameterizedTest
	@MethodSource("xmlAndXPathProvider")
	void testValidateFaultyInputForFind(String xml, String xPath, String expectedMessage) {
		assertThatExceptionOfType(XmlUtil.XmlException.class).isThrownBy(() -> XmlUtil.find(xml, xPath))
			.withMessage(expectedMessage);
	}

	@ParameterizedTest
	@MethodSource("xmlAndXPathProvider")
	void testValidateFaultyInputForRemove(String xml, String xPath, String expectedMessage) {
		assertThatExceptionOfType(XmlUtil.XmlException.class).isThrownBy(() -> XmlUtil.remove(xml, xPath))
			.withMessage(expectedMessage);
	}

	static Stream<Arguments> xmlAndXPathProvider() {
		final var xmlInputErrorMessage = "XML input cannot be null or blank";
		final var xPathInputErrorMessage = "XPath expression cannot be null or blank";
		return Stream.of(
			Arguments.of(null, "someXpath", xmlInputErrorMessage),
			Arguments.of("", "someXpath", xmlInputErrorMessage),
			Arguments.of(" ", "someXpath", xmlInputErrorMessage),
			Arguments.of("someXml", null, xPathInputErrorMessage),
			Arguments.of("someXml", "", xPathInputErrorMessage),
			Arguments.of("someXml", " ", xPathInputErrorMessage));
	}

	private String createFaultyXml() {
		return """
			<?xml version="1.0" encoding="UTF-8"?>
			<root>
				<parent>
					<child>some child content</child>
				</parent>
				<grandchild>some other content</grandchild>
			""";
	}
}
