package com.example.lending.loan.collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

class CollectionTaskStore {

    private static final Logger log = LoggerFactory.getLogger(CollectionTaskStore.class);

    private final Map<String, byte[]> db = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    boolean create(String taskId, Long loanId, long taskEpoch) {
        if (taskId == null || db.containsKey(taskId)) {
            return false;
        }
        long now = System.currentTimeMillis();
        try {
            db.put(taskId, encode(new TaskRecord(taskId, loanId, "OPEN", now, now, null, taskEpoch))
                    .getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            log.warn("task store create failed for {}: {}", taskId, e.getMessage());
            return false;
        }
    }

    public boolean updateStatus(String taskId, long expectedTaskEpoch, String newStatus, String output) {
        if (taskId == null || newStatus == null) {
            return false;
        }
        try {
            byte[] curBytes = db.get(taskId);
            if (curBytes == null) {
                return false;
            }
            TaskRecord cur = decode(new String(curBytes, StandardCharsets.UTF_8));
            if (cur.taskEpoch() != expectedTaskEpoch) {
                return false;
            }
            TaskRecord next = new TaskRecord(cur.taskId(), cur.loanId(), newStatus,
                cur.createdAtMs(), System.currentTimeMillis(), output, cur.taskEpoch());
            db.put(taskId, encode(next).getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            log.warn("task store updateStatus failed for {}: {}", taskId, e.getMessage());
            return false;
        }
    }

    private String encode(TaskRecord taskRecord) throws IOException {
        return mapper.writeValueAsString(taskRecord);
    }

    private TaskRecord decode(String json) throws IOException {
        return mapper.readValue(json, TaskRecord.class);
    }
}
