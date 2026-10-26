package com.example.lending.ops.part2.pretty;

import com.example.lending.ops.part2.pretty.PrettyFactory.Pretty;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

/** Indents XML (bureau reports, SOAP replies). Instances are handed out only by {@link PrettyFactory}. */
final class XmlPretty implements Pretty {

    @Override
    public  String format(String xmlString){
        try{
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(new InputSource(new StringReader(xmlString)));
            return format(document);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private String format(Document document) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }
}
