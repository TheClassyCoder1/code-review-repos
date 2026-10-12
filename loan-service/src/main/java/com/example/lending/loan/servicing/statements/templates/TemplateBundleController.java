package com.example.lending.loan.servicing.statements.templates;

import com.example.lending.loan.servicing.common.ServicingResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;

/** Validation of statement template bundles before they are activated. */
@RestController
@RequestMapping("/servicing/statements/templates")
public class TemplateBundleController {

    @PostMapping("check/bundle")
    @PreAuthorize("hasAuthority('template:manage')")
    public ServicingResult<Boolean> checkBundle(String bundle) throws IOException {
        TemplateBundles.requireCheckBundleFile(new File(bundle).toURI().toURL());
        return ServicingResult.ok(true);
    }
}
