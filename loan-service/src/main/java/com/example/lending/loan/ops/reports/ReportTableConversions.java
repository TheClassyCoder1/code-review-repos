package com.example.lending.loan.ops.reports;

import java.util.Date;
import java.util.Map;

final class ReportTableConversions {

    private ReportTableConversions() {
    }

    static ReportTableInfo mapToReportTableInfo(
            Map<String, Object> table, String database) {
        ReportTableInfo baseVO = new ReportTableInfo();
        baseVO.setCreateTime(new Date(Long.valueOf(table.get("CREATE_TIME").toString())));
        baseVO.setCreator((String) table.get("OWNER"));
        baseVO.setName((String) table.get("NAME"));
        baseVO.setDatabase(database);
        baseVO.setLatestAccessTime(new Date(Long.valueOf(table.get("LAST_ACCESS_TIME").toString())));
        return baseVO;
    }
}
