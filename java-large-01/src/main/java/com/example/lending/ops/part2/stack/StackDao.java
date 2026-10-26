package com.example.lending.ops.part2.stack;

import com.example.lending.ops.part2.stack.StackModel.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

/** JDBC access to the worker-host inventory. */
public interface StackDao {
    String COMPONENTS = "SELECT c.id, c.name, c.host_id, c.service_id, c.status, s.name AS service_name, "
            + "s.service_user, s.stack FROM ops_component c JOIN ops_service s ON s.id = c.service_id";

    @Repository
    @RequiredArgsConstructor
    class ComponentDao {
        private final JdbcTemplate jdbc;

        public List<ComponentPO> findAll() {
            return jdbc.query(COMPONENTS, new BeanPropertyRowMapper<>(ComponentPO.class));
        }

        public ComponentPO findDetailsById(Long id) {
            return jdbc.queryForObject(COMPONENTS + " WHERE c.id = ?", new BeanPropertyRowMapper<>(ComponentPO.class), id);
        }

        public void partialUpdateByIds(List<ComponentPO> list) {
            jdbc.batchUpdate("UPDATE ops_component SET status = ? WHERE id = ?", list.stream().map(c -> new Object[] {c.getStatus(), c.getId()}).toList());
        }
    }

    @Repository
    @RequiredArgsConstructor
    class HostDao {
        private final JdbcTemplate jdbc;

        public HostPO findById(Long id) {
            return jdbc.queryForObject("SELECT hostname, grpc_port FROM ops_host WHERE id = ?", new BeanPropertyRowMapper<>(HostPO.class), id);
        }
    }

    @Repository
    @RequiredArgsConstructor
    class ServiceDao {
        private final JdbcTemplate jdbc;

        public ServicePO findById(Long id) {
            return jdbc.queryForObject("SELECT id, status, restart_flag FROM ops_service WHERE id = ?", new BeanPropertyRowMapper<>(ServicePO.class), id);
        }

        public void partialUpdateById(ServicePO s) {
            jdbc.update("UPDATE ops_service SET status = ?, restart_flag = ? WHERE id = ?", s.getStatus(), s.getRestartFlag(), s.getId());
        }
    }
}
