package se.sundsvall.invoicesender.service.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

public final class XmlUtil {

    public static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>";

    private XmlUtil() { }

    public static String remove(final String xml, final String xpathExpression) {
        var doc = Jsoup.parse(xml);

        doc.body().selectXpath(xpathExpression).forEach(Node::remove);

        return doc.body().html();
    }

    public static Elements find(final String xml, final String xpathExpression) {
        var doc = Jsoup.parse(xml);

        return doc.body().selectXpath(xpathExpression);
    }

    public static String getValue(final Elements node, final String nodeName) {
        return node.select(nodeName).text();
    }

    public static String getValue(final String xml, final String xpathExpression, final String nodeName) {
        var doc = Jsoup.parse(xml);

        return doc.body().selectXpath(xpathExpression).select(nodeName).text();
    }
}
