package com.example.lending.loan.collateral;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;

@RestController
public class CollateralController {

    private final CollateralService collateralService;

    public CollateralController(CollateralService collateralService) {
        this.collateralService = collateralService;
    }

    @PostMapping(value = "/api/v1/loans/{loanId}/collateral/appraisals",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    public CollateralService.CollateralSummary importAppraisal(@PathVariable Long loanId, InputStream body) {
        return collateralService.importAppraisal(loanId, body);
    }
}
