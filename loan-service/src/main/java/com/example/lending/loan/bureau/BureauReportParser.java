package com.example.lending.loan.bureau;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Reads the credit bureau's XML report format (v3). */
@Component
public class BureauReportParser {

    public BureauReport parse(InputStream xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xml);

            Element root = document.getDocumentElement();
            String reference = requiredText(root, "BureauReference");
            long borrowerId = Long.parseLong(requiredText(root, "BorrowerId"));
            int score = Integer.parseInt(requiredText(root, "Score"));

            List<BureauReport.Tradeline> tradelines = new ArrayList<>();
            NodeList nodes = root.getElementsByTagName("Tradeline");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element node = (Element) nodes.item(i);
                tradelines.add(new BureauReport.Tradeline(
                        requiredText(node, "Type"),
                        new BigDecimal(requiredText(node, "Balance")),
                        new BigDecimal(requiredText(node, "CreditLimit"))));
            }
            return new BureauReport(reference, borrowerId, score, tradelines);
        } catch (ParserConfigurationException | SAXException | IOException | NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bureau report could not be read");
        }
    }

    private static String requiredText(Element parent, String tag) {
        NodeList matches = parent.getElementsByTagName(tag);
        if (matches.getLength() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bureau report is missing " + tag);
        }
        return matches.item(0).getTextContent().trim();
    }
}
