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
		var xpathExpressionFile1 = "//file[filename='Faktura_54225035_to_5502272684.pdf']";
		var xpathExpressionFile2 = "//file[filename='Faktura_54225036_to_5703122621.pdf']";

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
		var xpathExpression = "//file[filename='Faktura_54225041_to_5502272684.pdf']";

		var result = XmlUtil.find(originalXml, xpathExpression);

		assertThat(result).isNotNull();

		assertThat(XmlUtil.getChildNodeText(result, "filename")).isEqualTo("Faktura_54225041_to_5502272684.pdf");
		assertThat(XmlUtil.getChildNodeText(result, "idatakundnr")).isEqualTo("3910");
		assertThat(XmlUtil.getChildNodeText(result, "idatarutinnr")).isEqualTo("3910101");
		assertThat(XmlUtil.getChildNodeText(result, "fakturanr")).isEqualTo("54225041");
		assertThat(XmlUtil.getChildNodeText(result, "ocrnr")).isEqualTo("5422504109");
		assertThat(XmlUtil.getChildNodeText(result, "belopp_att_betala")).isEqualTo("1500.00");
		assertThat(XmlUtil.getChildNodeText(result, "kundid")).isEqualTo("5502272684");
		assertThat(XmlUtil.getChildNodeText(result, "fakturadatum")).isEqualTo("2025-08-01");
		assertThat(XmlUtil.getChildNodeText(result, "forfallodatum")).isEqualTo("2025-08-31");
		assertThat(XmlUtil.getChildNodeText(result, "kund_namn1")).isEqualTo("Testsson Test 1");
		assertThat(XmlUtil.getChildNodeText(result, "kund_namn2")).isBlank();
		assertThat(XmlUtil.getChildNodeText(result, "kund_postadress")).isEqualTo("Testgatan 1");
		assertThat(XmlUtil.getChildNodeText(result, "kund_postnummer")).isEqualTo("865 33");
		assertThat(XmlUtil.getChildNodeText(result, "kund_postort")).isEqualTo("Sundsvall");
		assertThat(XmlUtil.getChildNodeText(result, "kund_land")).isEqualTo("Sweden");
		assertThat(XmlUtil.getChildNodeText(result, "autogiro")).isEqualTo("AG");
		assertThat(XmlUtil.getChildNodeText(result, "ag_betalare")).isBlank();
		assertThat(XmlUtil.getChildNodeText(result, "retur")).isBlank();
		assertThat(XmlUtil.getChildNodeText(result, "gironr")).isEqualTo("5989-2810");
		assertThat(XmlUtil.getChildNodeText(result, "bankkod")).isBlank();
		assertThat(XmlUtil.getChildNodeText(result, "personnummer")).isBlank();
		assertThat(XmlUtil.getChildNodeText(result, "betalningssatt")).isBlank();
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

	@Test
	void testRemove_shouldThrowException_whenInvalidXPath() {
		var validXml = """
			<root>
				<parent>
					<child>some child content</child>
				</parent>
				<grandchild>some other content</grandchild>
			</root>""";
		assertThatExceptionOfType(XmlUtil.XmlException.class).isThrownBy(() -> XmlUtil.remove(validXml, "///invalidXpath"))
			.withMessage("Invalid XPath expression")
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
