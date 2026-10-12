package com.example.lending.loan.servicing.admin.roles;

import com.example.lending.loan.servicing.common.ServicingResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/** Role administration of the servicing back-office. */
@Controller
@RequestMapping("/servicing/admin/roles")
public class OperatorRoleController {

    private final OperatorRoleService roleService;
    private final RoleMembershipQueries membershipQueries;

    public OperatorRoleController(OperatorRoleService roleService, RoleMembershipQueries membershipQueries) {
        this.roleService = roleService;
        this.membershipQueries = membershipQueries;
    }

    @RequestMapping(value = "/allocResource", method = RequestMethod.POST)
    @ResponseBody
    public ServicingResult<Integer> allocResource(@RequestParam Long roleId, @RequestParam List<Long> resourceIds) {
        int count = roleService.allocResource(roleId, resourceIds);
        return ServicingResult.ok(count);
    }

    @RequestMapping(value = "/memberCount", method = RequestMethod.GET)
    @ResponseBody
    public ServicingResult<Long> memberCount(@RequestParam String roleKey) {
        return ServicingResult.ok(membershipQueries.countUMembers(roleKey));
    }
}
