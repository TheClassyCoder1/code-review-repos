package com.example.lending.loan.export;

import com.example.lending.loan.dto.ApiResult;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.*;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportExportController {

    private static final Logger log = LoggerFactory.getLogger(ReportExportController.class);
    private static final Pattern TEMPLATE_NAME = Pattern.compile("[a-z0-9][a-z0-9-]{0,63}\\.csv");

    private final CollectionsReportService collectionsReportService;
    private final ReportExportService exportService;
    private final ExportAttachmentStager attachmentStager;

    public ReportExportController(CollectionsReportService collectionsReportService,
                                  ReportExportService exportService, ExportAttachmentStager attachmentStager) {
        this.collectionsReportService = collectionsReportService;
        this.exportService = exportService;
        this.attachmentStager = attachmentStager;
    }

    @PostMapping("/collections")
    public ApiResult runCollectionsReport(@RequestHeader("X-User-Id") Long userId,
                                          @RequestBody CollectionsReportService.Request request) throws IOException {
        return ApiResult.success(collectionsReportService.run(userId, request));
    }

    @GetMapping("/exports")
    public List<ReportExportJob> listExports(@RequestHeader("X-User-Id") Long userId) {
        return exportService.listExports(userId);
    }

    @GetMapping("/exports/{exportId}/archive")
    public ResponseEntity<InputStreamResource> downloadArchive(@RequestHeader("X-User-Id") Long userId,
                                                               @PathVariable Long exportId, @RequestParam String name) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(exportService.openArchive(exportId, userId, name)));
    }

    @PostMapping("/exports/{exportId}/attachments")
    public ApiResult stageAttachments(@RequestHeader("X-User-Id") Long userId, @PathVariable Long exportId,
                                      @RequestBody ExportAttachmentStager.Attachments attachments) throws IOException {
        ReportExportJob job = exportService.getExport(exportId, userId);
        attachmentStager.stageAttachments(job.getId(), attachments);
        return ApiResult.success(null);
    }

    @PostMapping("/exports/copy")
    public ApiResult copyExportToAnotherUser(@RequestHeader("X-User-Id") Long userId, @RequestBody JsonNode jsonNode) {
        Long exportId = jsonNode.get("exportId").asLong();
        Long anotherUser = jsonNode.get("anotherUser").asLong();
        ApiResult result;
        try {
            log.info("User {} started copying report export {}", userId, exportId);
            Map<String, Object> properties = new HashMap<>();
            properties.put("copiedBy", userId);
            properties.put("system", "portal");
            ReportExportJob copy;
            synchronized (String.valueOf(exportId).intern()) {
                copy = exportService.copyExport(exportId, anotherUser, properties);
            }
            result = ApiResult.success(copy.getId());
        } catch (Exception e) {
            log.error("Failed to copy report export {}", exportId, e);
            result = ApiResult.error("Could not copy report export");
        }
        log.info("User {} finished copying report export {}", userId, exportId);
        return result;
    }

    @GetMapping("/templates/{fileName}")
    public void downloadTemplate(@PathVariable String fileName, HttpServletResponse response) throws IOException {
        if (!TEMPLATE_NAME.matcher(fileName).matches()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        ClassPathResource resource = new ClassPathResource("report-templates/" + fileName);
        response.setContentType("application/octet-stream; charset=UTF-8");
        StreamUtils.copy(resource.getInputStream(), response.getOutputStream());
    }
}
