package com.insurance.prospectus.controller;

import com.insurance.prospectus.dto.GenerateProspectusRequest;
import com.insurance.prospectus.dto.ProspectusDto;
import com.insurance.prospectus.service.ProspectusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prospectus")
@RequiredArgsConstructor
@Tag(name = "Prospectus", description = "Endpoints for generating and managing insurance prospectus documents")
public class ProspectusController {

    private final ProspectusService prospectusService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Generate a new prospectus")
    public ResponseEntity<ProspectusDto> generateProspectus(@Valid @RequestBody GenerateProspectusRequest request) {
        ProspectusDto prospectus = prospectusService.generateProspectus(request);
        return ResponseEntity.ok(prospectus);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get prospectus by ID")
    public ResponseEntity<ProspectusDto> getProspectus(@PathVariable Long id) {
        ProspectusDto prospectus = prospectusService.getProspectus(id);
        return ResponseEntity.ok(prospectus);
    }

    @GetMapping("/lead/{leadId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get all prospectus documents for a lead")
    public ResponseEntity<List<ProspectusDto>> getProspectusByLead(@PathVariable Long leadId) {
        List<ProspectusDto> prospectuses = prospectusService.getProspectusByLead(leadId);
        return ResponseEntity.ok(prospectuses);
    }

    @GetMapping("/{id}/html")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Get HTML content of prospectus")
    public ResponseEntity<String> getHtmlContent(@PathVariable Long id) {
        String html = prospectusService.getHtmlContent(id);
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Download prospectus as PDF")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        byte[] pdfContent = prospectusService.getPdfContent(id);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=prospectus_" + id + ".pdf")
            .body(pdfContent);
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Preview prospectus PDF in browser")
    public ResponseEntity<byte[]> previewPdf(@PathVariable Long id) {
        byte[] pdfContent = prospectusService.getPdfContent(id);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=prospectus_" + id + ".pdf")
            .body(pdfContent);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Delete a prospectus")
    public ResponseEntity<Void> deleteProspectus(@PathVariable Long id) {
        prospectusService.deleteProspectus(id);
        return ResponseEntity.noContent().build();
    }
}
