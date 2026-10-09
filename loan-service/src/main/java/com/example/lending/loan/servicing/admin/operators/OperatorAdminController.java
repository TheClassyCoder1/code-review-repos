package com.example.lending.loan.servicing.admin.operators;

import com.example.lending.loan.servicing.common.CurrentOperator;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Operator management screens of the back-office. */
@Controller
@RequestMapping("/servicing/admin/operators")
public class OperatorAdminController {

    private final OperatorAdminService userService;
    private final CurrentOperator currentOperator;

    public OperatorAdminController(OperatorAdminService userService, CurrentOperator currentOperator) {
        this.userService = userService;
        this.currentOperator = currentOperator;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('operator:view')")
    public String manageUsers(Model model) {
        model.addAttribute("users", userService.listUsers(currentOperator.tenantId()));
        return "admin/operators";
    }

    @RequestMapping("/deleteUser")
    @PreAuthorize("hasAuthority('operator:delete')")
    public String deleteUser(@RequestParam Long userId) {
        Assert.isTrue(userId != 1, "Cannot delete admin user");
        userService.deleteUser(currentOperator.tenantId(), userId);
        return "redirect:/servicing/admin/operators";
    }
}
