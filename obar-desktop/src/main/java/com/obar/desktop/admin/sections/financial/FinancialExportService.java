package com.obar.desktop.admin.sections.financial;

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
import com.obar.bll.admin.AdminFinancialOverviewDTO;
import com.obar.bll.admin.AdminPaymentByTripDTO;
import com.obar.bll.admin.AdminTaxRateDTO;
import com.obar.desktop.admin.shared.AdminFormatUtils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class FinancialExportService {

    private static final DateTimeFormatter EXPORT_READABLE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm",
            Locale.forLanguageTag("pt-PT"));

    void exportCsv(FinancialExportSnapshot snapshot, Path exportPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(exportPath, StandardCharsets.UTF_8)) {
            writer.write("Secao,Campo,Valor");
            writer.newLine();
            writer.write(csvLine("META", "Gerado em", EXPORT_READABLE_FORMAT.format(snapshot.generatedAt())));
            writer.newLine();
            writer.write(csvLine("META", "Periodo exportado", snapshot.scopeDescription()));
            writer.newLine();

            AdminFinancialOverviewDTO overview = snapshot.overview();
            writer.write(
                    csvLine("RESUMO", "Receita total", AdminFormatUtils.formatCurrency(overview.getTotalIncome())));
            writer.newLine();
            writer.write(
                    csvLine("RESUMO", "Receita periodo", AdminFormatUtils.formatCurrency(overview.getPeriodIncome())));
            writer.newLine();
            writer.write(csvLine("RESUMO", "Pagamentos totais", String.valueOf(overview.getTotalPayments())));
            writer.newLine();
            writer.write(csvLine("RESUMO", "Pagamentos processados", String.valueOf(overview.getProcessedPayments())));
            writer.newLine();
            writer.write(csvLine("RESUMO", "Pagamentos pendentes", String.valueOf(overview.getPendingPayments())));
            writer.newLine();
            writer.write(csvLine("RESUMO", "Pagamentos falhados", String.valueOf(overview.getFailedPayments())));
            writer.newLine();
            writer.write(csvLine("RESUMO", "Pagamentos reembolsados", String.valueOf(overview.getRefundedPayments())));
            writer.newLine();

            writer.newLine();
            writer.write("Estado,Total");
            writer.newLine();
            for (var status : overview.getPaymentStatuses()) {
                writer.write(csvLine(AdminFormatUtils.prettyPaymentStatus(status.getStatus()),
                        String.valueOf(status.getTotal())));
                writer.newLine();
            }

            writer.newLine();
            writer.write("Metodo,Total,Percentagem");
            writer.newLine();
            long paymentCount = snapshot.payments().size();
            for (Map.Entry<String, Long> method : snapshot.paymentMethods().entrySet().stream()
                    .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                    .toList()) {
                double pct = paymentCount == 0 ? 0 : (method.getValue() * 100.0) / paymentCount;
                writer.write(csvLine(AdminFormatUtils.prettyPaymentMethod(method.getKey()),
                        String.valueOf(method.getValue()), AdminFormatUtils.formatPercent(pct)));
                writer.newLine();
            }

            writer.newLine();
            writer.write("TaxaId,Nome,Valor,Descricao,Ativa");
            writer.newLine();
            for (AdminTaxRateDTO taxRate : snapshot.taxRates()) {
                writer.write(csvLine(
                        String.valueOf(taxRate.getId()),
                        AdminFormatUtils.fallback(taxRate.getName()),
                        taxRate.getRate() == null ? "-" : taxRate.getRate().toPlainString(),
                        AdminFormatUtils.fallback(taxRate.getDescription()),
                        Boolean.TRUE.equals(taxRate.getActive()) ? "Sim" : "Nao"));
                writer.newLine();
            }

            writer.newLine();
            writer.write("PagamentoId,ViagemId,Cliente,Motorista,Metodo,Valor,Moeda,Estado,Data,TaxaAplicada");
            writer.newLine();
            for (AdminPaymentByTripDTO payment : snapshot.payments()) {
                writer.write(csvLine(
                        payment.getPaymentId() == null ? "-" : String.valueOf(payment.getPaymentId()),
                        payment.getTripId() == null ? "-" : String.valueOf(payment.getTripId()),
                        AdminFormatUtils.fallback(payment.getClientName()),
                        AdminFormatUtils.fallback(payment.getDriverName()),
                        AdminFormatUtils.prettyPaymentMethod(payment.getPaymentMethodType()),
                        payment.getAmount() == null ? "0.00" : payment.getAmount().toPlainString(),
                        AdminFormatUtils.fallback(payment.getCurrencyCode()),
                        AdminFormatUtils.prettyPaymentStatus(payment.getStatus()),
                        payment.getPaymentDate() == null ? "-"
                                : EXPORT_READABLE_FORMAT.format(payment.getPaymentDate()),
                        payment.getTaxRateApplied() == null ? "-" : payment.getTaxRateApplied().toPlainString()));
                writer.newLine();
            }
        }
    }

    void exportPdf(FinancialExportSnapshot snapshot, Path exportPath) throws IOException, DocumentException {
        Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
        PdfWriter.getInstance(document, new FileOutputStream(exportPath.toFile()));
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph title = new Paragraph("OBAR - Relatorio Financeiro Completo", titleFont);
        title.setSpacingAfter(6f);
        document.add(title);
        document.add(new Paragraph("Gerado em: " + EXPORT_READABLE_FORMAT.format(snapshot.generatedAt()), bodyFont));
        document.add(new Paragraph("Periodo exportado: " + snapshot.scopeDescription(), bodyFont));
        document.add(new Paragraph(" "));

        Paragraph summarySectionTitle = new Paragraph("Resumo", sectionFont);
        summarySectionTitle.setSpacingAfter(8f);
        document.add(summarySectionTitle);
        PdfPTable summaryTable = new PdfPTable(new float[] { 3f, 2f, 2f, 2f, 2f, 2f, 2f });
        summaryTable.setWidthPercentage(100f);
        addPdfHeader(summaryTable, "Receita Total");
        addPdfHeader(summaryTable, "Receita Periodo");
        addPdfHeader(summaryTable, "Pagamentos");
        addPdfHeader(summaryTable, "Processados");
        addPdfHeader(summaryTable, "Pendentes");
        addPdfHeader(summaryTable, "Falhados");
        addPdfHeader(summaryTable, "Reembolsados");

        AdminFinancialOverviewDTO overview = snapshot.overview();
        addPdfCell(summaryTable, AdminFormatUtils.formatCurrency(overview.getTotalIncome()));
        addPdfCell(summaryTable, AdminFormatUtils.formatCurrency(overview.getPeriodIncome()));
        addPdfCell(summaryTable, String.valueOf(overview.getTotalPayments()));
        addPdfCell(summaryTable, String.valueOf(overview.getProcessedPayments()));
        addPdfCell(summaryTable, String.valueOf(overview.getPendingPayments()));
        addPdfCell(summaryTable, String.valueOf(overview.getFailedPayments()));
        addPdfCell(summaryTable, String.valueOf(overview.getRefundedPayments()));
        document.add(summaryTable);
        document.add(new Paragraph(" "));

        Paragraph methodsSectionTitle = new Paragraph("Distribuicao por Metodo", sectionFont);
        methodsSectionTitle.setSpacingAfter(8f);
        document.add(methodsSectionTitle);
        PdfPTable methodsTable = new PdfPTable(new float[] { 3f, 1f, 1f });
        methodsTable.setWidthPercentage(65f);
        addPdfHeader(methodsTable, "Metodo");
        addPdfHeader(methodsTable, "Total");
        addPdfHeader(methodsTable, "Percentagem");
        long paymentCount = snapshot.payments().size();
        for (Map.Entry<String, Long> method : snapshot.paymentMethods().entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .toList()) {
            double pct = paymentCount == 0 ? 0 : (method.getValue() * 100.0) / paymentCount;
            addPdfCell(methodsTable, AdminFormatUtils.prettyPaymentMethod(method.getKey()));
            addPdfCell(methodsTable, String.valueOf(method.getValue()));
            addPdfCell(methodsTable, AdminFormatUtils.formatPercent(pct));
        }
        document.add(methodsTable);
        document.add(new Paragraph(" "));

        Paragraph paymentsSectionTitle = new Paragraph("Pagamentos", sectionFont);
        paymentsSectionTitle.setSpacingAfter(8f);
        document.add(paymentsSectionTitle);
        PdfPTable paymentsTablePdf = new PdfPTable(
                new float[] { 1.1f, 1.1f, 2.1f, 2.1f, 1.6f, 1.3f, 0.9f, 1.2f, 1.6f, 1.1f });
        paymentsTablePdf.setWidthPercentage(100f);
        addPdfHeader(paymentsTablePdf, "Pagamento");
        addPdfHeader(paymentsTablePdf, "Viagem");
        addPdfHeader(paymentsTablePdf, "Cliente");
        addPdfHeader(paymentsTablePdf, "Motorista");
        addPdfHeader(paymentsTablePdf, "Metodo");
        addPdfHeader(paymentsTablePdf, "Valor");
        addPdfHeader(paymentsTablePdf, "Moeda");
        addPdfHeader(paymentsTablePdf, "Estado");
        addPdfHeader(paymentsTablePdf, "Data");
        addPdfHeader(paymentsTablePdf, "Taxa");

        for (AdminPaymentByTripDTO payment : snapshot.payments()) {
            addPdfCell(paymentsTablePdf, payment.getPaymentId() == null ? "-" : "#" + payment.getPaymentId());
            addPdfCell(paymentsTablePdf, payment.getTripId() == null ? "-" : "#" + payment.getTripId());
            addPdfCell(paymentsTablePdf, AdminFormatUtils.fallback(payment.getClientName()));
            addPdfCell(paymentsTablePdf, AdminFormatUtils.fallback(payment.getDriverName()));
            addPdfCell(paymentsTablePdf, AdminFormatUtils.prettyPaymentMethod(payment.getPaymentMethodType()));
            addPdfCell(paymentsTablePdf, payment.getAmount() == null ? "0.00" : payment.getAmount().toPlainString());
            addPdfCell(paymentsTablePdf, AdminFormatUtils.fallback(payment.getCurrencyCode()));
            addPdfCell(paymentsTablePdf, AdminFormatUtils.prettyPaymentStatus(payment.getStatus()));
            addPdfCell(paymentsTablePdf,
                    payment.getPaymentDate() == null ? "-" : EXPORT_READABLE_FORMAT.format(payment.getPaymentDate()));
            addPdfCell(paymentsTablePdf,
                    payment.getTaxRateApplied() == null ? "-" : payment.getTaxRateApplied().toPlainString());
        }
        document.add(paymentsTablePdf);

        document.add(new Paragraph(" "));
        Paragraph taxSectionTitle = new Paragraph("Taxas de IVA", sectionFont);
        taxSectionTitle.setSpacingAfter(8f);
        document.add(taxSectionTitle);
        PdfPTable taxTable = new PdfPTable(new float[] { 0.8f, 2f, 1.2f, 3f, 0.9f });
        taxTable.setWidthPercentage(100f);
        addPdfHeader(taxTable, "ID");
        addPdfHeader(taxTable, "Nome");
        addPdfHeader(taxTable, "Valor");
        addPdfHeader(taxTable, "Descricao");
        addPdfHeader(taxTable, "Ativa");
        for (AdminTaxRateDTO taxRate : snapshot.taxRates()) {
            addPdfCell(taxTable, taxRate.getId() == null ? "-" : String.valueOf(taxRate.getId()));
            addPdfCell(taxTable, AdminFormatUtils.fallback(taxRate.getName()));
            addPdfCell(taxTable, taxRate.getRate() == null ? "-" : taxRate.getRate().toPlainString());
            addPdfCell(taxTable, AdminFormatUtils.fallback(taxRate.getDescription()));
            addPdfCell(taxTable, Boolean.TRUE.equals(taxRate.getActive()) ? "Sim" : "Nao");
        }
        document.add(taxTable);

        document.close();
    }

    private void addPdfHeader(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
        cell.setBackgroundColor(new java.awt.Color(230, 234, 242));
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private void addPdfCell(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(
                new Phrase(value == null ? "-" : value, FontFactory.getFont(FontFactory.HELVETICA, 9)));
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private String csvLine(String... values) {
        return java.util.Arrays.stream(values)
                .map(this::escapeCsv)
                .collect(java.util.stream.Collectors.joining(","));
    }

    private String escapeCsv(String value) {
        String safe = value == null ? "" : value;
        boolean quote = safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r");
        if (quote) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    record FinancialExportSnapshot(
            LocalDateTime generatedAt,
            String scopeDescription,
            AdminFinancialOverviewDTO overview,
            List<AdminPaymentByTripDTO> payments,
            List<AdminTaxRateDTO> taxRates,
            Map<String, Long> paymentMethods) {
    }
}
