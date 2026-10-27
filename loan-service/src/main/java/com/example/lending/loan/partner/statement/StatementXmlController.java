package com.example.lending.loan.partner.statement;

import com.example.lending.loan.document.render.XmlPrettyPrinter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/partner/statements")
public class StatementXmlController {

    private final XmlPrettyPrinter xmlPrettyPrinter = new XmlPrettyPrinter();

    @PostMapping(value = "/xml/preview", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> preview(@RequestBody String xml) {
        if (xml.toUpperCase(Locale.ROOT).contains("<!DOCTYPE")) {
            return ResponseEntity.badRequest().body("Document type declarations are not accepted");
        }
        String formatted = xmlPrettyPrinter.format(xml);
        if (formatted == null) {
            return ResponseEntity.badRequest().body("Statement file is not well-formed XML");
        }
        return ResponseEntity.ok(formatted);
    }
}
