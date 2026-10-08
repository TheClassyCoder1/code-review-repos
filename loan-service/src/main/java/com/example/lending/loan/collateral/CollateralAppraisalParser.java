package com.example.lending.loan.collateral;

import com.example.lending.loan.config.XmlDocumentBuilders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Reads MISMO-style property appraisal exports from the valuation partner. */
@Component
public class CollateralAppraisalParser {

    private final XmlDocumentBuilders documentBuilders;

    public CollateralAppraisalParser(XmlDocumentBuilders documentBuilders) {
        this.documentBuilders = documentBuilders;
    }

    public Appraisal parse(InputStream xml) {
        try {
            DocumentBuilder builder = documentBuilders.newBuilder();
            Document document = builder.parse(xml);

            Element root = document.getDocumentElement();
            BigDecimal value = new BigDecimal(requiredText(root, "AppraisedValue"));
            if (value.signum() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appraised value must be positive");
            }
            return new Appraisal(
                    requiredText(root, "PropertyId"),
                    value,
                    LocalDate.parse(requiredText(root, "AppraisalDate")),
                    requiredText(root, "AppraiserLicense"));
        } catch (SAXException | IOException | NumberFormatException | DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appraisal could not be read");
        }
    }

    private static String requiredText(Element parent, String tag) {
        NodeList matches = parent.getElementsByTagName(tag);
        if (matches.getLength() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appraisal is missing " + tag);
        }
        return matches.item(0).getTextContent().trim();
    }

    public record Appraisal(String propertyId, BigDecimal appraisedValue,
                            LocalDate appraisalDate, String appraiserLicense) {
    }
}
