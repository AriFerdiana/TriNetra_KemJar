package com.smartwaste.controller;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.smartwaste.dto.response.WasteDepositResponse;
import com.smartwaste.service.WasteDepositService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;

@RestController
@RequestMapping("/citizen/export")
@PreAuthorize("hasRole('CITIZEN')")
public class PdfExportController {

    private final WasteDepositService depositService;

    public PdfExportController(WasteDepositService depositService) {
        this.depositService = depositService;
    }

    @GetMapping("/deposit/{id}/pdf")
    public ResponseEntity<byte[]> exportDepositPdf(@PathVariable String id) {
        WasteDepositResponse deposit = depositService.getById(id);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Header
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Paragraph title = new Paragraph("NetraSphere - Resi Setoran Sampah", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            document.add(new Paragraph(" "));
            document.add(new LineSeparator());
            document.add(new Paragraph(" "));

            // Info
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            document.add(new Paragraph("ID Setoran : " + deposit.getId(), boldFont));
            document.add(new Paragraph("Tanggal    : " + deposit.getCreatedAt().toString(), normalFont));
            document.add(new Paragraph("Kategori   : " + deposit.getCategoryName(), normalFont));
            document.add(new Paragraph("Berat      : " + deposit.getWeightKg() + " Kg", normalFont));
            document.add(new Paragraph("Poin       : " + deposit.getPointsEarned(), normalFont));
            document.add(new Paragraph("Status     : " + deposit.getStatus(), normalFont));
            document.add(new Paragraph("Lokasi     : " + deposit.getLocation(), normalFont));
            
            document.add(new Paragraph(" "));
            document.add(new LineSeparator());
            document.add(new Paragraph(" "));
            
            Paragraph footer = new Paragraph("Terima kasih telah berkontribusi menjaga bumi bersama NetraSphere!", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "resi-setoran-" + id + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(baos.toByteArray());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
