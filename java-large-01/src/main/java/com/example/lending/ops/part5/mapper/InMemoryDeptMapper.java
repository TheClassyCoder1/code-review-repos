package com.example.lending.ops.part5.mapper;

import com.example.lending.ops.part5.org.DeptTreeService;
import com.example.lending.ops.part5.org.SysDept;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/** Branch lookups served from the org tree; the table is small and changes rarely. */
@Component
@RequiredArgsConstructor
public class InMemoryDeptMapper implements BaseMapperX<SysDept> {

    private final DeptTreeService deptTreeService;

    @Override
    public List<SysDept> selectList(LambdaQueryWrapper<SysDept> queryWrapper) {
        return deptTreeService.loadAll().stream()
                .filter(queryWrapper::test)
                .collect(Collectors.toList());
    }
}
