package com.insurance.prospectus.service;

import com.insurance.common.entity.Lead;
import com.insurance.common.entity.Product;
import com.insurance.common.entity.Prospectus;
import com.insurance.common.entity.User;
import com.insurance.common.entity.VoiceSession;
import com.insurance.leads.repository.LeadRepository;
import com.insurance.auth.repository.UserRepository;
import com.insurance.products.repository.ProductRepository;
import com.insurance.voice.repository.VoiceSessionRepository;
import com.insurance.prospectus.dto.GenerateProspectusRequest;
import com.insurance.prospectus.dto.ProspectusDto;
import com.insurance.prospectus.repository.ProspectusRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProspectusService {

    private final ProspectusRepository prospectusRepository;
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final VoiceSessionRepository voiceSessionRepository;
    private final ProspectusTemplateService templateService;

    @Value("${app.storage.local-path:./storage}")
    private String storagePath;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Transactional
    public ProspectusDto generateProspectus(GenerateProspectusRequest request) {
        log.info("Generating prospectus for lead {}", request.getLeadId());

        Lead lead = leadRepository.findById(request.getLeadId())
            .orElseThrow(() -> new RuntimeException("Lead not found with id: " + request.getLeadId()));

        User agent = userRepository.findById(request.getAgentId())
            .orElseThrow(() -> new RuntimeException("Agent not found with id: " + request.getAgentId()));

        VoiceSession voiceSession = null;
        if (request.getVoiceSessionId() != null) {
            voiceSession = voiceSessionRepository.findById(request.getVoiceSessionId())
                .orElse(null);
        }

        // Get products
        List<Product> products = List.of();
        if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
            products = productRepository.findAllById(request.getProductIds());
        }

        // Gather customer needs
        Map<String, Object> customerNeeds = gatherCustomerNeeds(request, voiceSession);

        // Generate HTML
        String htmlContent = templateService.generateHtml(
            lead,
            agent,
            products,
            customerNeeds,
            request.getAdditionalNotes()
        );

        // Determine version
        List<Prospectus> existing = prospectusRepository.findByLeadIdOrderByVersionDesc(lead.getId());
        int version = existing.isEmpty() ? 1 : existing.get(0).getVersion() + 1;

        // Create prospectus entity
        Prospectus prospectus = Prospectus.builder()
            .lead(lead)
            .agent(agent)
            .voiceSession(voiceSession)
            .version(version)
            .htmlContent(htmlContent)
            .build();

        Prospectus saved = prospectusRepository.save(prospectus);

        // Generate PDF asynchronously (in a real app, this would be async)
        try {
            String pdfPath = generatePdf(saved.getId(), htmlContent);
            saved.setPdfPath(pdfPath);
            saved.setPdfUrl(frontendUrl + "/api/prospectus/" + saved.getId() + "/download");
            prospectusRepository.save(saved);
        } catch (Exception e) {
            log.error("Failed to generate PDF for prospectus {}", saved.getId(), e);
        }

        log.info("Generated prospectus {} for lead {}", saved.getId(), lead.getId());

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public ProspectusDto getProspectus(Long id) {
        Prospectus prospectus = prospectusRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Prospectus not found with id: " + id));
        return mapToDto(prospectus);
    }

    @Transactional(readOnly = true)
    public List<ProspectusDto> getProspectusByLead(Long leadId) {
        return prospectusRepository.findByLeadId(leadId).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public String getHtmlContent(Long id) {
        Prospectus prospectus = prospectusRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Prospectus not found with id: " + id));
        return prospectus.getHtmlContent();
    }

    @Transactional(readOnly = true)
    public byte[] getPdfContent(Long id) {
        Prospectus prospectus = prospectusRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Prospectus not found with id: " + id));

        if (prospectus.getPdfPath() == null) {
            throw new RuntimeException("PDF not generated for prospectus: " + id);
        }

        try {
            Path pdfPath = Paths.get(prospectus.getPdfPath());
            return Files.readAllBytes(pdfPath);
        } catch (Exception e) {
            log.error("Failed to read PDF file for prospectus {}", id, e);
            throw new RuntimeException("Failed to read PDF file", e);
        }
    }

    @Transactional
    public void deleteProspectus(Long id) {
        Prospectus prospectus = prospectusRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Prospectus not found with id: " + id));

        // Delete PDF file if exists
        if (prospectus.getPdfPath() != null) {
            try {
                Files.deleteIfExists(Paths.get(prospectus.getPdfPath()));
            } catch (Exception e) {
                log.error("Failed to delete PDF file for prospectus {}", id, e);
            }
        }

        prospectusRepository.deleteById(id);
        log.info("Deleted prospectus {}", id);
    }

    /**
     * Generate PDF from HTML using OpenHTMLtoPDF
     */
    private String generatePdf(Long prospectusId, String htmlContent) throws Exception {
        // Create storage directory if not exists
        Path storageDir = Paths.get(storagePath, "prospectus");
        Files.createDirectories(storageDir);

        // Generate PDF filename
        String filename = "prospectus_" + prospectusId + ".pdf";
        Path pdfPath = storageDir.resolve(filename);

        // Convert HTML to PDF
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(htmlContent, null);
            builder.toStream(os);
            builder.run();

            // Write to file
            try (FileOutputStream fos = new FileOutputStream(pdfPath.toFile())) {
                fos.write(os.toByteArray());
            }
        }

        log.info("Generated PDF for prospectus {} at {}", prospectusId, pdfPath);
        return pdfPath.toString();
    }

    /**
     * Gather customer needs from various sources
     */
    private Map<String, Object> gatherCustomerNeeds(
        GenerateProspectusRequest request,
        VoiceSession voiceSession
    ) {
        Map<String, Object> needs = new HashMap<>();

        // Add needs from request
        if (request.getCustomerNeeds() != null) {
            needs.putAll(request.getCustomerNeeds());
        }

        // Add needs from voice session
        if (voiceSession != null && voiceSession.getExtractedNeedsJson() != null) {
            needs.putAll(voiceSession.getExtractedNeedsJson());
        }

        return needs;
    }

    private ProspectusDto mapToDto(Prospectus prospectus) {
        return ProspectusDto.builder()
            .id(prospectus.getId())
            .leadId(prospectus.getLead().getId())
            .leadName(prospectus.getLead().getName())
            .agentId(prospectus.getAgent().getId())
            .agentName(prospectus.getAgent().getName())
            .voiceSessionId(prospectus.getVoiceSession() != null ? prospectus.getVoiceSession().getId() : null)
            .version(prospectus.getVersion())
            .htmlContent(prospectus.getHtmlContent())
            .pdfPath(prospectus.getPdfPath())
            .pdfUrl(prospectus.getPdfUrl())
            .createdAt(prospectus.getCreatedAt())
            .build();
    }
}
