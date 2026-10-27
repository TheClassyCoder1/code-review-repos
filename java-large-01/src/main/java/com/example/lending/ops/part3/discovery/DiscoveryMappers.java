package com.example.lending.ops.part3.discovery;

import com.example.lending.ops.part3.discovery.DiscoveryModel.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import java.util.Map;

/** Gateway discovery tables; column names follow the bean properties. */
public final class DiscoveryMappers {
    private DiscoveryMappers() { }
    @RequiredArgsConstructor
    abstract static class Base {
        final NamedParameterJdbcTemplate jdbc;
        <T> T first(String sql, Map<String, ?> args, Class<T> type) { return jdbc.query(sql, args, new BeanPropertyRowMapper<>(type)).stream().findFirst().orElse(null); }
        int insert(String table, String cols, String vals, Object row) { return jdbc.update("INSERT INTO " + table + " (" + cols + ") VALUES (" + vals + ")", new BeanPropertySqlParameterSource(row)); }
    }
    @Repository
    public static class DiscoveryMapper extends Base {
        DiscoveryMapper(NamedParameterJdbcTemplate jdbc) { super(jdbc); }
        public DiscoveryDO selectByPluginNameAndLevelAndNamespaceIdAndType(String plugin, String level, String ns, String type) {
            return first("SELECT * FROM discovery WHERE plugin_name = :p AND discovery_level = :l AND namespace_id = :n AND discovery_type = :t",
                    Map.of("p", plugin, "l", level, "n", ns, "t", type), DiscoveryDO.class);
        }
        public int insertSelective(DiscoveryDO d) {
            return insert("discovery", "id, discovery_name, plugin_name, discovery_type, discovery_level, server_list, props, namespace_id, "
                    + "date_created, date_updated", ":id, :discoveryName, :pluginName, :discoveryType, :discoveryLevel, :serverList, :props, "
                    + ":namespaceId, :dateCreated, :dateUpdated", d);
        }
    }
    @Repository
    public static class DiscoveryHandlerMapper extends Base {
        DiscoveryHandlerMapper(NamedParameterJdbcTemplate jdbc) { super(jdbc); }
        public DiscoveryHandlerDO selectBySelectorId(String selectorId) {
            return first("SELECT h.* FROM discovery_handler h JOIN discovery_rel r ON r.discovery_handler_id = h.id WHERE r.selector_id = :s",
                    Map.of("s", selectorId), DiscoveryHandlerDO.class);
        }
        public int insertSelective(DiscoveryHandlerDO h) {
            return insert("discovery_handler", "id, discovery_id, handler, listener_node, props, date_created",
                    ":id, :discoveryId, :handler, :listenerNode, :props, :dateCreated", h);
        }
    }
    @Repository
    public static class DiscoveryRelMapper extends Base {
        DiscoveryRelMapper(NamedParameterJdbcTemplate jdbc) { super(jdbc); }
        public int insertSelective(DiscoveryRelDO r) { return insert("discovery_rel", "id, discovery_handler_id, selector_id, plugin_name", ":id, :discoveryHandlerId, :selectorId, :pluginName", r); }
    }
}
