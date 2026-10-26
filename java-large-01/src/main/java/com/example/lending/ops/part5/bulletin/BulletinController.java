package com.example.lending.ops.part5.bulletin;

import com.example.lending.ops.part5.apidoc.Operation;
import com.example.lending.ops.part5.apidoc.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Dashboard bulletins; lives under /console so the session-based console chain applies. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/console/bulletins")
public class BulletinController {

    private final BulletinService bulletinService;

    @Operation(summary = "Delete Bulletins", description = "Delete Bulletin by ids")
    @DeleteMapping
    public ResponseEntity<Message<Void>> deleteBulletin(@Parameter(description = "Bulletin ids") @RequestParam List<Long> ids) {
        bulletinService.deleteBulletins(ids);
        return ResponseEntity.ok(Message.success("Delete success!"));
    }
}
