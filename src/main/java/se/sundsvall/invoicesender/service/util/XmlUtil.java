package se.sundsvall.invoicesender.service.util;

import static java.util.Optional.ofNullable;

import io.micrometer.common.util.StringUtils;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class XmlUtil {

	private static final Logger LOG = LoggerFactory.getLogger(XmlUtil.class);

	private static final String EMPTY_OR_BLANK_LINE = "(?m)^[ \t]*\r?\n";

	private XmlUtil() {}

	// ThreadLocal instances for XPath, DocumentBuilder, and Transformer to make them thread-safe
	private static final ThreadLocal<XPath> threadLocalXPath = ThreadLocal.withInitial(() -> {
		try {
			return XPathFactory.newInstance().newXPath();
		} catch (final Exception e) {
			throw new XmlException("Unable to create XPath instance", e);
		}
	});

	private static final ThreadLocal<DocumentBuilder> threadLocalDocumentBuilder = ThreadLocal.withInitial(() -> {
		try {
			final var documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			return documentBuilderFactory.newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			throw new XmlException("Unable to create DocumentBuilder instance", e);
		}
	});

	private static final ThreadLocal<Transformer> threadLocalTransformer = ThreadLocal.withInitial(() -> {
		try {
			final var transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

			final var transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			return transformer;
		} catch (final Exception e) {
			throw new XmlException("Unable to create TransformerFactory instance", e);
		}
	});

	private static XPath getXPath() {
		return threadLocalXPath.get();
	}

	private static DocumentBuilder getDocumentBuilder() {
		return threadLocalDocumentBuilder.get();
	}

	private static Transformer getTransformer() {
		return threadLocalTransformer.get();
	}

	/**
	 * Removes nodes from the given XML string that match the provided XPath expression.
	 *
	 * @param  xml             the XML string
	 * @param  xpathExpression the XPath expression
	 * @return                 the modified XML string
	 */
	public static String remove(final String xml, final String xpathExpression) {
		validateInput(xml, xpathExpression);
		LOG.info("Removing nodes from xml matching XPath {}", xpathExpression);
		try {
			final var document = toDocument(xml);

			// Find the matching nodes
			final var xPath = getXPath();
			final var nodesToRemove = (NodeList) xPath.evaluate(xpathExpression, document, XPathConstants.NODESET);

			// And remove them
			for (var i = 0; i < nodesToRemove.getLength(); i++) {
				final var nodeToRemove = nodesToRemove.item(i);
				nodeToRemove.getParentNode().removeChild(nodesToRemove.item(i));
			}

			return toString(document);
		} catch (final XPathExpressionException e) {
			throw new XmlException("Invalid XPath expression", e);
		} catch (final Exception e) {
			throw new XmlException("Unable to parse XML", e);
		} finally {
			threadLocalXPath.remove();
		}
	}

	/**
	 * Finds a single node in the given XML string that matches the provided XPath expression.
	 *
	 * @param  xml             the XML string
	 * @param  xpathExpression the XPath expression
	 * @return                 the matching node, or null if no match is found
	 */
	public static Node find(final String xml, final String xpathExpression) {
		validateInput(xml, xpathExpression);
		try {
			final var xPath = getXPath();
			final var document = toDocument(xml);

			return (Node) xPath.evaluate(xpathExpression, document, XPathConstants.NODE);
		} catch (final Exception e) {
			throw new XmlException("Unable to parse XML", e);
		} finally {
			threadLocalXPath.remove();
		}
	}

	/**
	 * Retrieves the text content of a child node with the specified name.
	 *
	 * @param  node          the parent node
	 * @param  childNodeName the name of the child node
	 * @return               the text content of the child node, or an empty string if not found
	 */
	public static String getChildNodeText(final Node node, final String childNodeName) {
		if (node == null || StringUtils.isBlank(childNodeName)) {
			LOG.warn("Invalid input: node or childNodeName is null or empty");
			return "";
		}

		final var childNodes = node.getChildNodes();
		for (var i = 0; i < childNodes.getLength(); i++) {
			final var childNode = childNodes.item(i);
			if (childNode.getNodeName().equals(childNodeName)) {
				return ofNullable(childNode.getTextContent()).map(String::trim).orElse("");
			}
		}

		return "";
	}

	private static Document toDocument(final String xml) throws IOException, SAXException {
		try (var stringReader = new StringReader(xml)) {
			final var inputSource = new InputSource(stringReader);
			return getDocumentBuilder().parse(inputSource);
		} finally {
			threadLocalDocumentBuilder.remove();
		}
	}

	private static String toString(final Document document) {
		LOG.debug("Converting Document to XML string");

		try (var stringWriter = new StringWriter()) {
			getTransformer().transform(new DOMSource(document), new StreamResult(stringWriter));
			return stringWriter.toString().replaceAll(EMPTY_OR_BLANK_LINE, "");
		} catch (TransformerException | IOException e) {
			throw new XmlException("Unable to convert XML document to string", e);
		} finally {
			threadLocalTransformer.remove();
		}
	}

	/**
	 * Sanity check input parameters.
	 *
	 * @param xml             the XML string
	 * @param xpathExpression the XPath expression
	 */
	private static void validateInput(final String xml, final String xpathExpression) {
		if (StringUtils.isBlank(xml)) {
			throw new XmlException("XML input cannot be null or blank");
		}
		if (StringUtils.isBlank(xpathExpression)) {
			throw new XmlException("XPath expression cannot be null or blank");
		}
	}

	public static class XmlException extends RuntimeException {
		private static final long serialVersionUID = 3792852597914506707L;

		public XmlException(final String message) {
			super(message);
		}

		public XmlException(final String message, final Throwable cause) {
			super(message, cause);
		}
	}
}
