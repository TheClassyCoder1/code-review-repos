package com.example.lending.ops.part3.sortgroup;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;

/** Reads the event-routing groups that decide which sink each loan event stream is sorted into. */
@Repository
@RequiredArgsConstructor
public class InlongGroupEntityMapper {
    private final TransactionBoundCursors cursors;
    public Cursor<SortSourceGroupInfo> selectAllGroups() {
        return cursors.open("SELECT group_id, cluster_tag FROM event_route_group WHERE status = 'ACTIVE'",
                (rs, i) -> new SortSourceGroupInfo(rs.getString("group_id"), rs.getString("cluster_tag")));
    }
    public record SortSourceGroupInfo(String groupId, String clusterTag) { }
    public interface ConfigLoader { List<SortSourceGroupInfo> loadAllGroup(); }
}
