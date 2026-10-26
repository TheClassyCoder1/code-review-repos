package com.example.lending.ops.part5.maintenance;

import com.example.lending.ops.part5.maintenance.web.PageParam;
import com.example.lending.ops.part5.maintenance.web.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/** Queries maintenance record lines for the branch equipment screens. */
@Service
@RequiredArgsConstructor
public class MesDvMaintenRecordLineService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PageResult<MesDvMaintenRecordLineDO> getMaintenRecordLinePage(MesDvMaintenRecordLinePageReqVO req) {
        String where = " WHERE deleted = false"
                + (req.getRecordId() != null ? " AND record_id = :recordId" : "")
                + (req.getResult() != null ? " AND result = :result" : "");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("recordId", req.getRecordId())
                .addValue("result", req.getResult());
        Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM dv_mainten_record_line" + where, params, Long.class);
        String sql = "SELECT * FROM dv_mainten_record_line" + where + " ORDER BY id DESC";
        if (req.getPageSize() != PageParam.PAGE_SIZE_NONE) {
            sql += " LIMIT :limit OFFSET :offset";
            params.addValue("limit", req.getPageSize())
                    .addValue("offset", (long) (req.getPageNo() - 1) * req.getPageSize());
        }
        return new PageResult<>(jdbcTemplate.query(sql, params,
                new BeanPropertyRowMapper<>(MesDvMaintenRecordLineDO.class)), total == null ? 0 : total);
    }
}
