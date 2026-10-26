package com.example.lending.ops.part5.mapper;

import com.example.lending.ops.part5.common.CollUtil;

import java.util.Collection;
import java.util.List;

/** Common query shortcuts for console reference-data mappers. */
public interface BaseMapperX<T> {

    List<T> selectList(LambdaQueryWrapper<T> queryWrapper);

    default List<T> selectList(SFunction<T, ?> field, Collection<?> values) {
        if (CollUtil.isEmpty(values)) {
            return CollUtil.newArrayList();
        }
        return selectList(new LambdaQueryWrapper<T>().in(field, values));
    }
}
