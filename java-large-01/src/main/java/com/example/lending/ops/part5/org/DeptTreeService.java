package com.example.lending.ops.part5.org;

import com.example.lending.ops.part5.common.CollUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resolves the branch subtree used to scope portfolio reports: a regional
 * manager sees the loans booked by their branch and every branch below it.
 */
@Service
@RequiredArgsConstructor
public class DeptTreeService {

    private final JdbcTemplate jdbcTemplate;

    public List<SysDept> loadAll() {
        return jdbcTemplate.query(
                "SELECT dept_id, parent_id, code, sort_order FROM sys_dept WHERE deleted = false",
                (rs, i) -> new SysDept(rs.getLong("dept_id"), rs.getLong("parent_id"),
                        rs.getString("code"), rs.getInt("sort_order")));
    }

	public List<SysDept> selectChildrenByDept(long deptId, List<SysDept> allDeptList) {
		List<SysDept> list = new ArrayList<>();
		Optional<SysDept> optionalDept = allDeptList.stream().filter(w -> w.getDeptId() == deptId).findFirst();

		optionalDept.ifPresent(list::add);

		List<SysDept> collect = allDeptList.stream().filter(w -> w.getParentId() == deptId).toList();

		if (CollUtil.isEmpty(collect)) {
			return list;
		}

		collect.forEach(dept -> {
			List<SysDept> depts = selectChildrenByDept(dept.getDeptId(), allDeptList);
			list.addAll(depts);
		});

		return list;
	}
}
