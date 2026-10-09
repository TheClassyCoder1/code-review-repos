package com.example.lending.loan.servicing.statements;

import com.example.lending.loan.servicing.common.ServicingResult;
import com.example.lending.loan.servicing.statements.xml.StatementXmlExporter;
import javax.xml.stream.XMLStreamException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Back-office statement operations. */
@RestController
@RequestMapping("/servicing/statements")
public class StatementController {

    private final StatementService statementService;
    private final StatementXmlExporter xmlExporter;

    public StatementController(StatementService statementService, StatementXmlExporter xmlExporter) {
        this.statementService = statementService;
        this.xmlExporter = xmlExporter;
    }

    @GetMapping(value = "/{id}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    @PreAuthorize("hasAuthority('statement:read')")
    public ResponseEntity<String> xml(@PathVariable Long id) throws XMLStreamException {
        return ResponseEntity.ok(xmlExporter.export(statementService.get(id)));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('statement:publish')")
    public ServicingResult<Void> publish(@PathVariable Long id) {
        statementService.publish(id);
        return ServicingResult.ok();
    }
}
