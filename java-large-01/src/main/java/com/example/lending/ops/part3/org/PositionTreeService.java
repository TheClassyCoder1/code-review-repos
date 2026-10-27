package com.example.lending.ops.part3.org;

import com.example.lending.ops.part3.org.SysPositionSelectTreeVo.SysDepartPositionVo;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

/** Builds the position picker tree; rows are mapped by the row mapper and null rows never reach the tree nodes. */
@Service
@RequiredArgsConstructor
public class PositionTreeService {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    public List<SysPositionSelectTreeVo> positionTree(String branchId) {
        List<SysDepartPositionVo> rows = jdbcTemplate.query("SELECT p.id, p.position_name, d.depart_name, p.dep_post_parent_id, p.iz_leaf "
                + "FROM branch_position p JOIN branch d ON d.id = p.depart_id WHERE d.id = :branchId", Map.of("branchId", branchId), new BeanPropertyRowMapper<>(SysDepartPositionVo.class));
        return rows.stream().filter(Objects::nonNull).map(SysPositionSelectTreeVo::new).toList();
    }
}
