package com.example.lending.loan.ops.setup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

@RestController
@RequestMapping("/ops/setup")
public class SetupWizardController {

    private static final Logger log = LoggerFactory.getLogger(SetupWizardController.class);

    @PostMapping("/db/validate")
    public SetupValidationResult validateDatabase(@RequestBody DbConnectionForm props) {
        SetupValidationResult form = new SetupValidationResult();
        onValidateModelObjects(props, form);
        return form;
    }

    private void onValidateModelObjects(DbConnectionForm props, SetupValidationResult form) {
        try {
            Class.forName(props.getDriver());
        } catch (Exception e) {
            form.error("No driver available for " + props.getDbName());
            return;
        }
        boolean valid = true;
        try {
            props.updateUrl();
        } catch (IllegalArgumentException e) {
            form.error(e.getMessage());
            return;
        }
        DriverManager.setLoginTimeout(3);
        try (Connection conn = DriverManager.getConnection(props.getURL(), props.getLogin(), props.getPassword())) {
            valid = conn.isValid(0);
            String sql = null;
            switch (props.getDbType()) {
                case DB2:
                    sql = "select count(*) from systables";
                    break;
                case ORACLE:
                    sql = "SELECT 1 FROM DUAL";
                    break;
                default:
                    sql = "SELECT 1";
                    break;
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                valid &= ps.execute();
            }
            if (!valid) {
                form.error("Connection is not valid: " + props.getDbName());
            }
        } catch (Exception e) {
            form.error(e.getMessage() + " - " + props.getDbName());
            log.error("error while checking the database connection", e);
            valid = false;
        }
        if (valid) {
            form.success("Database connection is valid");
        }
    }
}
