package com.example.lending.ops.part2.sqoop;

import com.example.lending.ops.part2.sqoop.SqoopParameters.SqoopTaskExecutionContext;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

/** Runs the nightly import of core-banking tables into the reporting lake. */
@Component
public class SqoopImportTask {

    private final OracleSourceGenerator oracleSourceGenerator = new OracleSourceGenerator();

    public int run(SqoopParameters parameters, SqoopTaskExecutionContext context, String hiveTable) throws Exception {
        if (hiveTable == null || !hiveTable.matches("[a-z][a-z0-9_]{0,63}(\\.[a-z][a-z0-9_]{0,63})?")) {
            throw new IllegalArgumentException("hive table name is invalid");
        }
        SqoopCommandGuard.check(parameters, context);
        String command = "/opt/sqoop/bin/sqoop import -D mapred.job.name=" + parameters.getJobName()
                + oracleSourceGenerator.generate(parameters, context) + " --hive-import --hive-table " + hiveTable;
        Process process = new ProcessBuilder("bash", "-c", command).inheritIO().start();
        if (!process.waitFor(2, TimeUnit.HOURS)) {
            process.destroyForcibly();
            throw new IllegalStateException("sqoop import timed out");
        }
        return process.exitValue();
    }
}
