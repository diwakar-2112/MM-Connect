package com.MM_CONNECT.api.admin.report;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/reports")
public class AdminReportController {
    private final AdminReportService reports;
    public AdminReportController(AdminReportService reports) { this.reports = reports; }

    @GetMapping
    public ResponseEntity<ReportDtos.Catalog> catalog(@AuthenticationPrincipal UUID actorId) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(reports.catalog(actorId));
    }
    @GetMapping("/{reportId}")
    public ResponseEntity<ReportDtos.ReportPage> report(@AuthenticationPrincipal UUID actorId,
            @PathVariable String reportId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(reports.report(actorId, reportId, page, size));
    }
}
