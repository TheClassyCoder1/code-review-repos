package com.example.lending.ops.part5.org;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A branch or team in the lending organisation tree. Root nodes use
 * {@code parentId = 0}; ids are never null.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SysDept {
    private long deptId;
    private long parentId;
    private String code;
    private int sortOrder;
}
