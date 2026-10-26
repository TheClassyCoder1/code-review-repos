package com.example.lending.ops.backup;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.Set;

/**
 * Restores metadata from a peer node. The source host must be one of the configured cluster peers
 * ({@code ops.cluster.peer-hosts}); any other host is refused before a connection is attempted.
 */
@RestController
@RequiredArgsConstructor
public class MetadataBackupSyncService {

    public record PullRequest(String fromHost, String tmpFilePath, Long tmpFileSize, String resourceGroupId) {
    }

    private final MetadataBackupFileService fileService;
    @Value("${ops.cluster.peer-hosts}")
    private final Set<String> peerHosts;

    @PostMapping("/console/backup/pull")
    public String pullFromPeer(@RequestBody PullRequest r) throws JsonProcessingException {
        if (!peerHosts.contains(r.fromHost())) {
            throw new IllegalArgumentException("source host is not a cluster peer");
        }
        return fileService.downloadMetadataBackTmpFile(r.tmpFilePath(), r.tmpFileSize(), r.resourceGroupId(), r.fromHost());
    }
}
