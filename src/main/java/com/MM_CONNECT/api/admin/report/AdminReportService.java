package com.MM_CONNECT.api.admin.report;

import com.MM_CONNECT.api.admin.StaffAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static com.MM_CONNECT.api.admin.report.ReportDtos.*;

@Service
public class AdminReportService {
    private static final String REPORT_ID = "signed-in-users";
    private static final String TITLE = "Signed-in users";
    private static final Map<String, String> STATUS_LABELS = Map.of(
            "NOT_SUBMITTED", "Not submitted", "PENDING", "Awaiting approval",
            "APPROVED", "Approved", "CHANGES_REQUIRED", "Corrections requested",
            "SUPERSEDED", "Superseded");
    private static final List<Column> COLUMNS = List.of(
            new Column("name", "Name", DataType.TEXT, true, Map.of()),
            new Column("email", "Email", DataType.EMAIL, true, Map.of()),
            new Column("createdAt", "Created at", DataType.DATETIME, false, Map.of()),
            new Column("lastLoginAt", "Last login", DataType.DATETIME, true, Map.of()),
            new Column("onboarded", "Onboarded", DataType.BOOLEAN, false, Map.of()),
            new Column("onboardingStatus", "Onboarding status", DataType.STATUS, false, STATUS_LABELS),
            new Column("verified", "Verified", DataType.BOOLEAN, false, Map.of()));
    private final StaffAccessService access;
    private final MemberReportRepository members;

    public AdminReportService(StaffAccessService access, MemberReportRepository members) {
        this.access = access; this.members = members;
    }

    @Transactional(readOnly = true)
    public Catalog catalog(UUID actorId) {
        access.requireReportReader(actorId);
        return new Catalog(List.of(new Node("members", "Members", NodeKind.GROUP, null,
                List.of(new Node(REPORT_ID, TITLE, NodeKind.REPORT, REPORT_ID, List.of())))));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReportPage report(UUID actorId, String reportId, int page, int size) {
        access.requireReportReader(actorId);
        if (!REPORT_ID.equals(reportId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found.");
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Use page >= 0 and size between 1 and 100.");
        long total = members.countMembers();
        long offset = (long) page * size;
        var rows = offset >= total ? List.<Map<String, Object>>of()
                : members.findMembers(size, offset).stream().map(this::values).toList();
        int totalPages = (int) Math.min(Integer.MAX_VALUE, total / size + (total % size == 0 ? 0 : 1));
        return new ReportPage(REPORT_ID, TITLE, COLUMNS, rows, page, size, total, totalPages,
                offset >= total || total - offset <= size);
    }

    private Map<String, Object> values(MemberReportRepository.MemberRow member) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("rowId", member.id());
        row.put("name", member.name()); row.put("email", member.email());
        row.put("createdAt", member.createdAt()); row.put("lastLoginAt", member.lastLoginAt());
        row.put("onboarded", member.onboarded()); row.put("onboardingStatus", member.onboardingStatus());
        row.put("verified", member.verified());
        // Map.copyOf rejects nullable names/emails/timestamps.
        return Collections.unmodifiableMap(row);
    }
}
