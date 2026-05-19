package com.smartwaste.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.smartwaste.entity.WasteDeposit;
import com.smartwaste.repository.WasteDepositRepository;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfExportService {

    private final WasteDepositRepository wasteDepositRepository;

    public PdfExportService(WasteDepositRepository wasteDepositRepository) {
        this.wasteDepositRepository = wasteDepositRepository;
    }

    public byte[] exportDepositsToPdf() {
        List<WasteDeposit> deposits = wasteDepositRepository.findAll();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            Font tableBodyFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);

            // Title
            Paragraph title = new Paragraph("Laporan Setoran Sampah - SmartWaste System", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            // Table setup
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 2f, 2f, 1.5f, 1f, 2f, 2f}); // Column widths

            // Headers
            String[] headers = {"ID Setoran", "Warga", "Kategori", "Berat (Kg)", "Poin", "Status", "Tanggal"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, tableHeaderFont));
                cell.setBackgroundColor(new Color(16, 185, 129)); // Emerald 500
                cell.setPadding(8f);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Data
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            for (WasteDeposit deposit : deposits) {
                addCell(table, deposit.getId(), tableBodyFont);
                addCell(table, deposit.getCitizen() != null ? deposit.getCitizen().getName() : "-", tableBodyFont);
                addCell(table, deposit.getCategory() != null ? deposit.getCategory().getName() : "-", tableBodyFont);
                addCell(table, String.valueOf(deposit.getWeightKg()), tableBodyFont);
                addCell(table, String.valueOf(deposit.getPointsEarned()), tableBodyFont);
                addCell(table, deposit.getStatus() != null ? deposit.getStatus().name() : "-", tableBodyFont);
                addCell(table, deposit.getCreatedAt() != null ? deposit.getCreatedAt().format(dtf) : "-", tableBodyFont);
            }

            document.add(table);
            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    /** Export laporan harian khusus untuk seorang Collector */
    public byte[] exportCollectorDailyReport(com.smartwaste.entity.Collector collector) {
        java.time.LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
        java.time.LocalDateTime endOfDay = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);

        // Ambil setoran yang dikonfirmasi/ditolak oleh collector ini hari ini
        List<WasteDeposit> deposits = wasteDepositRepository.findAll().stream()
                .filter(d -> d.getCollector() != null && d.getCollector().getId().equals(collector.getId()))
                .filter(d -> d.getConfirmedAt() != null
                        && !d.getConfirmedAt().isBefore(startOfDay)
                        && !d.getConfirmedAt().isAfter(endOfDay))
                .toList();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
            Font subFont    = FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(100, 100, 100));
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
            Font bodyFont   = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

            DateTimeFormatter dtf  = DateTimeFormatter.ofPattern("dd MMMM yyyy", new java.util.Locale("id", "ID"));
            DateTimeFormatter dtfTime = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

            // Header
            Paragraph title = new Paragraph("Laporan Harian Petugas — NetraSphere", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4f);
            document.add(title);

            Paragraph sub = new Paragraph("Petugas: " + collector.getName() + "  |  Area: " + (collector.getAssignedArea() != null ? collector.getAssignedArea() : "-") + "  |  Tanggal: " + java.time.LocalDate.now().format(dtf), subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(20f);
            document.add(sub);

            // Summary stats
            long totalConfirmed = deposits.stream().filter(d -> d.getStatus() == com.smartwaste.entity.enums.DepositStatus.CONFIRMED).count();
            long totalRejected  = deposits.stream().filter(d -> d.getStatus() == com.smartwaste.entity.enums.DepositStatus.REJECTED).count();
            double totalWeight  = deposits.stream().mapToDouble(WasteDeposit::getWeightKg).sum();
            double totalPoints  = deposits.stream().mapToDouble(WasteDeposit::getPointsEarned).sum();

            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(20f);
            String[] sumLabels = {"✅ Dikonfirmasi", "❌ Ditolak", "⚖️ Total Berat", "🏅 Total Poin"};
            String[] sumValues = {totalConfirmed + " setoran", totalRejected + " setoran", String.format("%.1f kg", totalWeight), String.format("%.0f poin", totalPoints)};
            for (int i = 0; i < 4; i++) {
                PdfPCell lbl = new PdfPCell(new Phrase(sumLabels[i], headerFont));
                lbl.setBackgroundColor(new Color(16, 185, 129));
                lbl.setPadding(8f);
                lbl.setHorizontalAlignment(Element.ALIGN_CENTER);
                summaryTable.addCell(lbl);
            }
            for (String v : sumValues) {
                PdfPCell valCell = new PdfPCell(new Phrase(v, bodyFont));
                valCell.setPadding(8f);
                valCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                summaryTable.addCell(valCell);
            }
            document.add(summaryTable);

            // Detail table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2.5f, 2f, 1f, 1f, 1.5f, 2f});

            String[] headers = {"Warga", "Kategori", "Berat (kg)", "Poin", "Status", "Waktu Proses"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(52, 52, 52));
                cell.setPadding(7f);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }
            for (WasteDeposit d : deposits) {
                addCell(table, d.getCitizen() != null ? d.getCitizen().getName() : "-", bodyFont);
                addCell(table, d.getCategory() != null ? d.getCategory().getName() : "-", bodyFont);
                addCell(table, String.format("%.1f", d.getWeightKg()), bodyFont);
                addCell(table, String.format("%.0f", d.getPointsEarned()), bodyFont);
                addCell(table, d.getStatus() != null ? d.getStatus().name() : "-", bodyFont);
                addCell(table, d.getConfirmedAt() != null ? d.getConfirmedAt().format(dtfTime) : "-", bodyFont);
            }
            document.add(table);
            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    /** Export seluruh riwayat setoran untuk seorang Collector */
    public byte[] exportCollectorFullHistory(com.smartwaste.entity.Collector collector) {
        List<WasteDeposit> deposits = wasteDepositRepository.findAll().stream()
                .filter(d -> d.getCollector() != null && d.getCollector().getId().equals(collector.getId()))
                .filter(d -> d.getStatus() != com.smartwaste.entity.enums.DepositStatus.PENDING)
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .toList();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
            Font subFont    = FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(100, 100, 100));
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
            Font bodyFont   = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

            DateTimeFormatter dtfTime = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

            Paragraph title = new Paragraph("Laporan Riwayat Kerja Petugas — NetraSphere", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4f);
            document.add(title);

            Paragraph sub = new Paragraph("Petugas: " + collector.getName() + "  |  Total Aktivitas: " + deposits.size() + "  |  Dicetak: " + java.time.LocalDateTime.now().format(dtfTime), subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(20f);
            document.add(sub);

            long totalConfirmed = deposits.stream().filter(d -> d.getStatus() == com.smartwaste.entity.enums.DepositStatus.CONFIRMED).count();
            long totalRejected  = deposits.stream().filter(d -> d.getStatus() == com.smartwaste.entity.enums.DepositStatus.REJECTED).count();
            double totalWeight  = deposits.stream().mapToDouble(WasteDeposit::getWeightKg).sum();
            double totalPoints  = deposits.stream().mapToDouble(WasteDeposit::getPointsEarned).sum();

            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(20f);
            String[] sumLabels = {"Dikonfirmasi", "Ditolak", "Total Berat", "Total Poin"};
            String[] sumValues = {totalConfirmed + " setoran", totalRejected + " setoran", String.format("%.1f kg", totalWeight), String.format("%.0f poin", totalPoints)};
            for (int i = 0; i < 4; i++) {
                PdfPCell lbl = new PdfPCell(new Phrase(sumLabels[i], headerFont));
                lbl.setBackgroundColor(new Color(16, 185, 129));
                lbl.setPadding(8f);
                lbl.setHorizontalAlignment(Element.ALIGN_CENTER);
                summaryTable.addCell(lbl);
            }
            for (String v : sumValues) {
                PdfPCell valCell = new PdfPCell(new Phrase(v, bodyFont));
                valCell.setPadding(8f);
                valCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                summaryTable.addCell(valCell);
            }
            document.add(summaryTable);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2.5f, 2f, 1f, 1f, 1.5f, 2f});
            String[] headers = {"Warga", "Kategori", "Berat (kg)", "Poin", "Status", "Waktu Proses"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(52, 52, 52));
                cell.setPadding(7f);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }
            for (WasteDeposit d : deposits) {
                addCell(table, d.getCitizen() != null ? d.getCitizen().getName() : "-", bodyFont);
                addCell(table, d.getCategory() != null ? d.getCategory().getName() : "-", bodyFont);
                addCell(table, String.format("%.1f", d.getWeightKg()), bodyFont);
                addCell(table, String.format("%.0f", d.getPointsEarned()), bodyFont);
                addCell(table, d.getStatus() != null ? d.getStatus().name() : "-", bodyFont);
                addCell(table, d.getConfirmedAt() != null ? d.getConfirmedAt().format(dtfTime) : "-", bodyFont);
            }
            document.add(table);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6f);
        table.addCell(cell);
    }

    /** Export Green Receipt (Bukti Digital) untuk Warga */
    public byte[] exportGreenReceipt(WasteDeposit deposit) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A5);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(16, 185, 129)); // Emerald 500
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font highlightFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(16, 185, 129));

            // Header
            Paragraph title = new Paragraph("GREEN RECEIPT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("NetraSphere Smart Waste System", subTitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(15f);
            document.add(subtitle);

            // Receipt Info
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", new java.util.Locale("id", "ID"));
            document.add(new Paragraph("ID Setoran : " + deposit.getId(), bodyFont));
            document.add(new Paragraph("Tanggal    : " + (deposit.getConfirmedAt() != null ? deposit.getConfirmedAt().format(dtf) : "-"), bodyFont));
            document.add(new Paragraph("Warga      : " + (deposit.getCitizen() != null ? deposit.getCitizen().getName() : "-"), bodyFont));
            document.add(new Paragraph("Kategori   : " + (deposit.getCategory() != null ? deposit.getCategory().getName() : "-"), bodyFont));
            document.add(new Paragraph("Berat      : " + String.format("%.2f kg", deposit.getWeightKg()), highlightFont));
            document.add(new Paragraph("Poin Didapat: " + String.format("%.0f pts", deposit.getPointsEarned()), highlightFont));
            document.add(new Paragraph("\n"));

            // Eco-Impact Calculation
            double weight = deposit.getWeightKg();
            double co2Saved = 0;
            double waterSaved = 0;
            double treesSaved = 0;
            String categoryName = deposit.getCategory() != null ? deposit.getCategory().getName().toLowerCase() : "";

            if (categoryName.contains("plastik")) {
                co2Saved = weight * 2.5;
            } else if (categoryName.contains("kertas") || categoryName.contains("kardus")) {
                co2Saved = weight * 0.9;
                waterSaved = weight * 26.5;
                treesSaved = weight / 60.0;
            } else if (categoryName.contains("organik")) {
                co2Saved = weight * 0.5;
                waterSaved = weight * 2.0;
            } else if (categoryName.contains("logam") || categoryName.contains("besi")) {
                co2Saved = weight * 4.0;
                waterSaved = weight * 14.0;
            }

            Paragraph impactTitle = new Paragraph("Dampak Lingkungan Anda:", subTitleFont);
            impactTitle.setSpacingAfter(5f);
            document.add(impactTitle);

            PdfPTable impactTable = new PdfPTable(3);
            impactTable.setWidthPercentage(100);
            String[] impLabels = {"CO2 Dicegah", "Air Dihemat", "Pohon Diselamatkan"};
            String[] impValues = {
                    String.format("%.1f kg", co2Saved),
                    String.format("%.0f L", waterSaved),
                    String.format("%.2f", treesSaved)
            };

            for (String lbl : impLabels) {
                PdfPCell cell = new PdfPCell(new Phrase(lbl, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
                cell.setBackgroundColor(new Color(16, 185, 129));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5f);
                impactTable.addCell(cell);
            }
            for (String val : impValues) {
                PdfPCell cell = new PdfPCell(new Phrase(val, bodyFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5f);
                impactTable.addCell(cell);
            }
            document.add(impactTable);

            // Footer
            Paragraph footer = new Paragraph("\nTerima kasih telah berkontribusi untuk bumi yang lebih hijau!", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }
}
