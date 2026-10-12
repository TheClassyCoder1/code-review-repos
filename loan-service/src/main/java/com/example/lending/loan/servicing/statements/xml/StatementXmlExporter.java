package com.example.lending.loan.servicing.statements.xml;

import com.example.lending.loan.servicing.statements.model.Statement;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;

/** Serializes statements to indented XML for partner banks and archiving systems. */
@Component
public class StatementXmlExporter {

    private final XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();

    public String export(Statement statement) throws XMLStreamException {
        StringWriter writer = new StringWriter();
        XMLStreamWriter xml = outputFactory.createXMLStreamWriter(writer);
        xml.writeStartDocument("UTF-8", "1.0");
        xml.writeStartElement("statement");
        xml.writeAttribute("id", String.valueOf(statement.getId()));
        xml.writeAttribute("version", String.valueOf(statement.getVersion()));
        element(xml, "loanId", String.valueOf(statement.getLoanId()));
        element(xml, "period", statement.getPeriod());
        element(xml, "openingBalance", statement.getOpeningBalance().toPlainString());
        element(xml, "closingBalance", statement.getClosingBalance().toPlainString());
        element(xml, "totalPaid", statement.getTotalPaid().toPlainString());
        xml.writeEndElement();
        xml.writeEndDocument();
        xml.close();
        return StatementXmlFormatter.transformer(writer.toString());
    }

    private static void element(XMLStreamWriter xml, String name, String value) throws XMLStreamException {
        xml.writeStartElement(name);
        xml.writeCharacters(value);
        xml.writeEndElement();
    }
}
