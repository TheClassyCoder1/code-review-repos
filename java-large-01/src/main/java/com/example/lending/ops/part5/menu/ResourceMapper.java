package com.example.lending.ops.part5.menu;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Loads the menu resources granted to a console login through its roles. */
@Repository
@RequiredArgsConstructor
public class ResourceMapper {

    private final JdbcTemplate jdbcTemplate;

    public List<ResourceDO> selectByUserName(String userName) {
        return jdbcTemplate.query(
                "SELECT DISTINCT r.id, r.parent_id, r.title, r.name, r.url, r.resource_type, r.sort, r.perms "
                        + "FROM console_resource r "
                        + "JOIN console_role_resource rr ON rr.resource_id = r.id "
                        + "JOIN console_user_role ur ON ur.role_id = rr.role_id "
                        + "JOIN console_users u ON u.id = ur.user_id "
                        + "WHERE u.login_name = ? AND r.deleted = false ORDER BY r.sort",
                (rs, i) -> new ResourceDO(rs.getString("id"), rs.getString("parent_id"), rs.getString("title"),
                        rs.getString("name"), rs.getString("url"), rs.getInt("resource_type"), rs.getInt("sort"),
                        rs.getString("perms")),
                userName);
    }
}
