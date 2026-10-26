package com.example.lending.ops.part2.admin;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

/** Back-office maintenance of featured products, order fees, roles, categories and dictionaries. */
@Service
@RequiredArgsConstructor
public class AdminConsoleService {
    private final NamedParameterJdbcTemplate jdbc;

    /** Response envelope of the system-settings endpoints. */
    public record R<T>(int code, String msg, T data) {
        static <T> R<T> ok(T data) { return new R<>(0, "success", data); }
        static <T> R<T> failed(String msg) { return new R<>(1, msg, null); }
    }

    @Data
    public static class OmsMoneyInfoParam {
        private Long orderId;
        private BigDecimal originationFee, discountAmount;
    }

    @Data
    public static class PmsProductCategoryParam {
        private Long parentId;
        private String name;
        private Integer sort, showStatus;
    }

    public int updateRecommendStatus(List<Long> ids, Integer status) {
        return jdbc.update("UPDATE ops_home_new_product SET recommend_status = :status WHERE id IN (:ids) AND :status IN (0, 1)",
                new MapSqlParameterSource("status", status).addValue("ids", ids));
    }

    /** Fees can only change while the disbursement order is pending funding (status 0). */
    public int updateMoneyInfo(OmsMoneyInfoParam p) {
        return jdbc.update("UPDATE ops_disbursement_order SET origination_fee = :fee, discount_amount = :discount, "
                + "payable_amount = principal_amount + :fee - :discount WHERE id = :id AND status = 0 AND :fee >= 0 AND :discount >= 0",
                new MapSqlParameterSource("fee", p.getOriginationFee()).addValue("discount", p.getDiscountAmount()).addValue("id", p.getOrderId()));
    }

    @Transactional
    public int allocMenu(Long roleId, List<Long> menuIds) {
        jdbc.update("DELETE FROM ops_role_menu_relation WHERE role_id = :role", new MapSqlParameterSource("role", roleId));
        return jdbc.batchUpdate("INSERT INTO ops_role_menu_relation (role_id, menu_id) VALUES (:role, :menu)", menuIds.stream().distinct()
                .map(menu -> new MapSqlParameterSource("role", roleId).addValue("menu", menu)).toArray(SqlParameterSource[]::new)).length;
    }

    public int update(Long id, PmsProductCategoryParam p) {
        return jdbc.update("UPDATE ops_product_category SET parent_id = COALESCE(:parent, 0), name = :name, sort = :sort, "
                + "show_status = :show WHERE id = :id AND :id <> COALESCE(:parent, 0) AND length(trim(:name)) > 0",
                new MapSqlParameterSource("parent", p.getParentId()).addValue("name", p.getName()).addValue("sort", p.getSort())
                        .addValue("show", p.getShowStatus()).addValue("id", id));
    }

    /** Soft-deletes a dictionary item; items of system dictionaries are kept. */
    public R<Boolean> removeDictItem(Long id) {
        int count = jdbc.update("UPDATE ops_dict_item i SET del_flag = true FROM ops_dict d "
                + "WHERE i.id = :id AND d.id = i.dict_id AND d.system_flag = false", new MapSqlParameterSource("id", id));
        return count > 0 ? R.ok(true) : R.failed("dictionary item not found or protected");
    }
}
