package com.example.lending.loan.servicing.collections.documents;

import com.example.lending.loan.servicing.collections.debtors.DebtorCommandGateway;
import com.example.lending.loan.servicing.collections.debtors.DebtorCommandGateway.CreateIdentificationCardScanCommand;
import com.example.lending.loan.servicing.collections.debtors.DebtorService;
import com.example.lending.loan.servicing.common.ServicingException;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/** Identification card scans collected during collections onboarding. */
@Controller
@Validated
@RequestMapping("/servicing/collections/debtors")
public class BorrowerDocumentController {

    private static final long MAX_SCAN_BYTES = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_PDF_VALUE);

    private final DebtorService debtorService;
    private final DebtorDocumentService documentService;
    private final DebtorCommandGateway commandGateway;

    public BorrowerDocumentController(DebtorService debtorService, DebtorDocumentService documentService,
                                      DebtorCommandGateway commandGateway) {
        this.debtorService = debtorService;
        this.documentService = documentService;
        this.commandGateway = commandGateway;
    }

    @PostMapping(value = "/{identifier}/identifications/{number}/scans", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('debtor:documents')")
    @ResponseBody
    ResponseEntity<Void> postIdentificationCardScan(@PathVariable("identifier") final String identifier,
                                                    @PathVariable("number") final String number,
                                                    @RequestParam("scanIdentifier") @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$") final String scanIdentifier,
                                                    @RequestParam("description") @Size(max = 4096) final String description,
                                                    @RequestPart("image") final MultipartFile image) throws Exception {
        this.throwIfDebtorNotExists(identifier);
        this.throwIfIdentificationCardNotExists(identifier, number);
        this.throwIfInvalidSize(image.getSize());
        this.throwIfInvalidContentType(image.getContentType());

        if (this.documentService.identificationCardScanExists(number, scanIdentifier)) {
            throw ServicingException.conflict("Scan " + scanIdentifier + " already exists.");
        }

        final IdentityDocumentScan scan = new IdentityDocumentScan();
        scan.setIdentifier(scanIdentifier);
        scan.setDescription(description);

        this.commandGateway.process(new CreateIdentificationCardScanCommand(number, scan, image.getBytes(), image.getContentType()));

        return ResponseEntity.accepted().build();
    }


    private void throwIfDebtorNotExists(String identifier) {
        if (!debtorService.debtorExists(identifier)) {
            throw ServicingException.notFound("Debtor " + identifier + " not found.");
        }
    }

    private void throwIfIdentificationCardNotExists(String identifier, String number) {
        if (!documentService.identificationCardExists(identifier, number)) {
            throw ServicingException.notFound("Identification card " + number + " not found.");
        }
    }

    private void throwIfInvalidSize(long size) {
        if (size <= 0 || size > MAX_SCAN_BYTES) {
            throw ServicingException.badRequest("Scan must be between 1 byte and 2 MB.");
        }
    }

    private void throwIfInvalidContentType(String contentType) {
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw ServicingException.badRequest("Unsupported scan content type.");
        }
    }
}
