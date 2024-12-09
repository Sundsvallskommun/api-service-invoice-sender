package se.sundsvall.invoicesender.service.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import se.sundsvall.dept44.test.annotation.resource.Load;
import se.sundsvall.dept44.test.extension.ResourceLoaderExtension;

@ExtendWith(ResourceLoaderExtension.class)
class XmlUtilTests {

	@Test
	void testRemove(@Load("/xmlutil/input.xml") final String input,
		@Load("/xmlutil/expected.xml") final String expected) {
		var xpathExpression = "//item[@id=2] | //item[@id=3]";
		var result = XmlUtil.remove(input, xpathExpression);

		assertThat(result).isEqualToIgnoringWhitespace(expected);
	}

	@Test
	void testFind(@Load("/xmlutil/input.xml") final String xml) {
		var xpathExpression = "//item[@id=2]";
		var result = XmlUtil.find(xml, xpathExpression);

		assertThat(result).isNotNull().isNotEmpty();
	}

	@Test
	void testGetValueFromXml(@Load("/xmlutil/input.xml") final String xml) {
		var xpathExpression = "//item[@id=4]";
		var result = XmlUtil.getValue(xml, xpathExpression, "name");

		assertThat(result).isEqualTo("item 4");

		result = XmlUtil.getValue(xml, xpathExpression, "non-existent");
		assertThat(result).isEmpty();
	}

	@Test
	void testGetValueFromNode(@Load("/xmlutil/input.xml") final String xml) {
		var xpathExpression = "//item[@id=2]";
		var node = XmlUtil.find(xml, xpathExpression);

		var result = XmlUtil.getValue(node, "name");

		assertThat(result).isEqualTo("item 2");

		result = XmlUtil.getValue(node, "non-existent");
		assertThat(result).isEmpty();
	}
}
