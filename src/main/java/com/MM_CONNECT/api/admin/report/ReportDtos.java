package com.MM_CONNECT.api.admin.report;

import java.util.List;
import java.util.Map;

/** Declarative display metadata, never executable HTML, SQL or renderer code. */
public final class ReportDtos {
    private ReportDtos() {}

    public enum NodeKind { GROUP, REPORT }
    public enum DataType { TEXT, EMAIL, DATETIME, BOOLEAN, STATUS }
    public record Node(String id, String label, NodeKind kind, String reportId, List<Node> children) {}
    public record Catalog(List<Node> items) {}
    public record Column(String key, String label, DataType dataType, boolean nullable,
                         Map<String, String> valueLabels) {}
    public record ReportPage(String reportId, String title, List<Column> columns,
                             List<Map<String, Object>> content, int pageNumber, int pageSize,
                             long totalElements, int totalPages, boolean isLast) {}
}
