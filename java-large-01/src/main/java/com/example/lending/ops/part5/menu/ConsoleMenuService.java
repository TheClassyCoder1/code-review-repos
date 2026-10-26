package com.example.lending.ops.part5.menu;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/** Builds the menu entries the console front end renders for a login. */
@Service
@RequiredArgsConstructor
public class ConsoleMenuService {

    private final ResourceMapper resourceMapper;
    private final DashboardProperties dashboardProperties;

    public List<ResourceVO> getMenuInfo(final String userName) {
        return getResourceListByUserName(userName);
    }

    private List<ResourceVO> getResourceListByUserName(final String userName) {
        if (SessionUtil.isAdmin()) {
            return ListUtil.map(resourceMapper.selectByUserName(userName), ResourceVO::buildResourceVO);
        }
        // filter [Only the super administrator root user has the privileges]
        return resourceMapper.selectByUserName(userName)
                .stream()
                .filter(r -> !dashboardProperties.getOnlySuperAdminPermission().contains(r.getPerms()))
                .map(ResourceVO::buildResourceVO)
                .collect(Collectors.toList());
    }
}
