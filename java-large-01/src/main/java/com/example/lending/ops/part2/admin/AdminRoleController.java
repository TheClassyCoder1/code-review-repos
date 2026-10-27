package com.example.lending.ops.part2.admin;

import com.example.lending.ops.common.Result.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/ops/admin/role")
@RequiredArgsConstructor
public class AdminRoleController {
    private final AdminConsoleService roleService;

    @Operation(summary = "给角色分配菜单")
    @RequestMapping(value = "/allocMenu", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult allocMenu(@RequestParam Long roleId, @RequestParam List<Long> menuIds) {
        int count = roleService.allocMenu(roleId, menuIds);
        return CommonResult.success(count);
    }
}
