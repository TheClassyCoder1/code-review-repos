package com.example.lending.ops.part3.sortgroup;

import com.example.lending.ops.part3.sortgroup.InlongGroupEntityMapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Feeds the sort-config snapshot the event routers poll. */
@Service
@RequiredArgsConstructor
public class ConfigLoaderImpl implements ConfigLoader {
    private final InlongGroupEntityMapper inlongGroupEntityMapper;
    @Transactional
    @Override
    public List<SortSourceGroupInfo> loadAllGroup() {
        Cursor<SortSourceGroupInfo> cursor = inlongGroupEntityMapper.selectAllGroups();
        List<SortSourceGroupInfo> allGroups = new ArrayList<>();
        cursor.forEach(allGroups::add);
        return allGroups;
    }
}
