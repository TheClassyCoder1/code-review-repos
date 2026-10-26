package com.example.lending.ops.part3.org;

import lombok.Data;

/** Node of the branch/position picker used when assigning loan officers. */
@Data
public class SysPositionSelectTreeVo {
    private String value, title, id, parentId, orgCategory;
    private boolean isLeaf, checkable, disabled;
    public SysPositionSelectTreeVo(SysDepartPositionVo position) {
        this.value = position.getId();
        if(oConvertUtils.isNotEmpty(position.getDepartName())){
            this.title = position.getPositionName() + "("+position.getDepartName()+")";
        }else{
            this.title = position.getPositionName();
        }
        this.id = position.getId();
        this.parentId = position.getDepPostParentId();
        this.orgCategory = "3";
        if(position!=null && position.getIzLeaf()!=null && 0 == position.getIzLeaf()){
            this.isLeaf = false;
        }else{
            this.isLeaf = true;
        }
        this.checkable = true;
        this.disabled = false;
    }
    @Data
    public static class SysDepartPositionVo {
        private String id, positionName, departName, depPostParentId;
        private Integer izLeaf;
    }
    static final class oConvertUtils {
        static boolean isNotEmpty(Object v) { return v != null && !v.toString().isBlank(); }
    }
}
