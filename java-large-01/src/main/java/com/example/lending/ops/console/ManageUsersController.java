package com.example.lending.ops.console;

import com.example.lending.ops.security.ConsoleSecurityConfig.RequiresPermissions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/console/s")
@RequiredArgsConstructor
public class ManageUsersController {

    private final ConsoleUserService userService;

    @RequestMapping("/deleteUser")
    @RequiresPermissions("user:delete")
    public String deleteUser(@RequestParam Long userId) {
        Assert.isTrue(userId != 1, "Cannot delete admin user");
        userService.deleteUser(userId);
        return "redirect:/s/manageUsers";
    }
}
