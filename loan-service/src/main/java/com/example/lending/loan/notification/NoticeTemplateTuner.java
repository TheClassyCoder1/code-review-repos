package com.example.lending.loan.notification;

import org.springframework.stereotype.Component;
import org.w3c.dom.*;

import javax.xml.xpath.*;

/** Applies the servicing sender identity to a notice template's header block. */
@Component
public class NoticeTemplateTuner {

    static final String FROM_NAME = "Lending Servicing";
    static final String REPLY_TO = "servicing@lending.example.com";

    Document updateSenderBlock(Document document) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();

        Node fieldNode = (Node) xPath.evaluate("//header/field[@value=\"sender\"]", document, XPathConstants.NODE);
        Element headerNode = (Element) fieldNode.getParentNode();

        NodeList children = headerNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element element)) {
                continue;
            }
            if (element.getAttribute("name").equals("from-name")) {
                element.setAttribute("value", FROM_NAME);
            }
            if (element.getAttribute("name").equals("reply-to")) {
                element.setAttribute("value", REPLY_TO);
            }
        }

        return document;
    }
}
