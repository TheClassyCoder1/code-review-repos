package com.example.lending.ops.part5.bulletin;

import com.example.lending.ops.part5.common.CollUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** Persistence for dashboard bulletins (disbursement freezes, bureau outages). */
@Service
@RequiredArgsConstructor
public class BulletinService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void deleteBulletins(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        jdbcTemplate.update("UPDATE ops_bulletin SET deleted = true WHERE id IN (:ids)", Map.of("ids", ids));
    }
}
