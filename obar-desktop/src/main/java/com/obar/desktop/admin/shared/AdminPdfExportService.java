package com.obar.desktop.admin.shared;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Shared PDF table exporter for admin desktop dashboards.
 */
public final class AdminPdfExportService {

    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss",
            Locale.ROOT);
    private static final DateTimeFormatter READABLE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm",
            Locale.forLanguageTag("pt-PT"));

    public <T> Path exportTable(
            String title,
            String scopeDescription,
            String filePrefix,
            List<PdfColumn<T>> columns,
            List<T> rows) throws IOException, DocumentException {
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("Export columns must not be empty.");
        }

        Path exportPath = resolveExportPath(filePrefix);
        Document document = new Document(columns.size() > 12 ? PageSize.A3.rotate() : PageSize.A4.rotate(), 24, 24,
                24, 24);
        try (FileOutputStream output = new FileOutputStream(exportPath.toFile())) {
            PdfWriter.getInstance(document, output);
            document.open();
            try {
                writeContent(document, title, scopeDescription, columns, rows == null ? List.of() : rows);
            } finally {
                if (document.isOpen()) {
                    document.close();
                }
            }
        }
        return exportPath;
    }

    private <T> void writeContent(
            Document document,
            String title,
            String scopeDescription,
            List<PdfColumn<T>> columns,
            List<T> rows) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph reportTitle = new Paragraph(AdminFormatUtils.fallback(title), titleFont);
        reportTitle.setSpacingAfter(6f);
        document.add(reportTitle);
        document.add(new Paragraph("Gerado em: " + READABLE_TIMESTAMP_FORMAT.format(LocalDateTime.now()), bodyFont));
        document.add(new Paragraph(AdminFormatUtils.fallback(scopeDescription), bodyFont));
        document.add(new Paragraph("Registos exportados: " + rows.size(), bodyFont));
        document.add(new Paragraph(" "));

        if (rows.isEmpty()) {
            document.add(new Paragraph("Sem dados para exportar.", bodyFont));
            return;
        }

        PdfPTable table = new PdfPTable(columnWidths(columns));
        table.setWidthPercentage(100f);
        table.setHeaderRows(1);

        for (PdfColumn<T> column : columns) {
            addHeader(table, column.title(), columns.size());
        }

        for (T row : rows) {
            for (PdfColumn<T> column : columns) {
                addCell(table, column.value(row), columns.size());
            }
        }

        document.add(table);
    }

    private float[] columnWidths(List<? extends PdfColumn<?>> columns) {
        float[] widths = new float[columns.size()];
        for (int index = 0; index < columns.size(); index++) {
            widths[index] = columns.get(index).width();
        }
        return widths;
    }

    private void addHeader(PdfPTable table, String value, int columnCount) {
        PdfPCell cell = new PdfPCell(
                new Phrase(safe(value), FontFactory.getFont(FontFactory.HELVETICA_BOLD, tableFontSize(columnCount))));
        cell.setBackgroundColor(new Color(230, 234, 242));
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String value, int columnCount) {
        PdfPCell cell = new PdfPCell(
                new Phrase(safe(value), FontFactory.getFont(FontFactory.HELVETICA, tableFontSize(columnCount))));
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private int tableFontSize(int columnCount) {
        if (columnCount > 18) {
            return 6;
        }
        if (columnCount > 12) {
            return 7;
        }
        return 8;
    }

    private Path resolveExportPath(String filePrefix) throws IOException {
        Path exportDirectory = Path.of(System.getProperty("user.home"), "Documents", "obar-exports");
        Files.createDirectories(exportDirectory);
        String safePrefix = AdminFormatUtils.normalize(filePrefix).replaceAll("[^a-z0-9_-]+", "_");
        if (safePrefix.isBlank()) {
            safePrefix = "admin_export";
        }
        return exportDirectory.resolve(safePrefix + "_" + FILE_TIMESTAMP_FORMAT.format(LocalDateTime.now()) + ".pdf");
    }

    private String safe(String value) {
        return AdminFormatUtils.fallback(value).replace("\r", " ").replace("\n", " ");
    }

    public static <T> PdfColumn<T> column(String title, float width, Function<T, String> valueProvider) {
        return new PdfColumn<>(title, width, valueProvider);
    }

    public record PdfColumn<T>(String title, float width, Function<T, String> valueProvider) {

        public String value(T item) {
            return valueProvider == null ? "-" : valueProvider.apply(item);
        }
    }
}
