package com.insurance.prospectus.service;

import com.insurance.common.entity.Lead;
import com.insurance.common.entity.Product;
import com.insurance.common.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Service for creating HTML templates for prospectus documents
 */
@Service
@Slf4j
public class ProspectusTemplateService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    /**
     * Generate HTML for prospectus
     */
    public String generateHtml(
        Lead lead,
        User agent,
        List<Product> products,
        Map<String, Object> customerNeeds,
        String additionalNotes
    ) {
        log.info("Generating prospectus HTML for lead {} with {} products", lead.getId(), products.size());

        StringBuilder html = new StringBuilder();

        html.append(getHtmlHeader());
        html.append(generateCoverPage(lead, agent));
        html.append(generateNeedsSummary(customerNeeds));
        html.append(generateProductComparison(products));
        html.append(generateAdditionalNotes(additionalNotes));
        html.append(generateFooter(agent));
        html.append(getHtmlFooter());

        return html.toString();
    }

    private String getHtmlHeader() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Insurance Proposal</title>
                <style>
                    @page {
                        size: A4;
                        margin: 2cm;
                    }
                    body {
                        font-family: 'Helvetica', 'Arial', sans-serif;
                        line-height: 1.6;
                        color: #333;
                        margin: 0;
                        padding: 20px;
                    }
                    .cover-page {
                        text-align: center;
                        padding: 100px 0;
                        page-break-after: always;
                    }
                    .cover-page h1 {
                        font-size: 36px;
                        color: #2c3e50;
                        margin-bottom: 20px;
                    }
                    .cover-page .client-name {
                        font-size: 24px;
                        color: #3498db;
                        margin: 30px 0;
                    }
                    .section {
                        margin: 40px 0;
                        page-break-inside: avoid;
                    }
                    .section h2 {
                        font-size: 24px;
                        color: #2c3e50;
                        border-bottom: 2px solid #3498db;
                        padding-bottom: 10px;
                        margin-bottom: 20px;
                    }
                    .needs-list {
                        background: #f8f9fa;
                        padding: 20px;
                        border-radius: 5px;
                        margin: 20px 0;
                    }
                    .needs-list .need-item {
                        margin: 10px 0;
                        padding: 10px;
                        background: white;
                        border-left: 4px solid #3498db;
                    }
                    .product-comparison {
                        width: 100%;
                        border-collapse: collapse;
                        margin: 20px 0;
                    }
                    .product-comparison th {
                        background: #3498db;
                        color: white;
                        padding: 12px;
                        text-align: left;
                        font-weight: bold;
                    }
                    .product-comparison td {
                        padding: 12px;
                        border-bottom: 1px solid #ddd;
                    }
                    .product-comparison tr:nth-child(even) {
                        background: #f8f9fa;
                    }
                    .product-card {
                        border: 1px solid #ddd;
                        border-radius: 5px;
                        padding: 20px;
                        margin: 20px 0;
                        background: white;
                    }
                    .product-card h3 {
                        color: #2c3e50;
                        margin-top: 0;
                    }
                    .product-card .insurer {
                        color: #7f8c8d;
                        font-style: italic;
                    }
                    .footer {
                        margin-top: 50px;
                        padding-top: 20px;
                        border-top: 1px solid #ddd;
                        text-align: center;
                        color: #7f8c8d;
                        font-size: 12px;
                    }
                    .agent-info {
                        margin: 30px 0;
                        padding: 20px;
                        background: #f8f9fa;
                        border-radius: 5px;
                    }
                </style>
            </head>
            <body>
            """;
    }

    private String getHtmlFooter() {
        return """
            </body>
            </html>
            """;
    }

    private String generateCoverPage(Lead lead, User agent) {
        String date = LocalDateTime.now().format(DATE_FORMATTER);

        return String.format("""
            <div class="cover-page">
                <h1>Insurance Proposal</h1>
                <div class="client-name">Prepared for<br>%s</div>
                <div style="margin-top: 50px;">
                    <p><strong>Date:</strong> %s</p>
                    <p><strong>Prepared by:</strong> %s</p>
                </div>
            </div>
            """, lead.getName(), date, agent.getName());
    }

    private String generateNeedsSummary(Map<String, Object> customerNeeds) {
        if (customerNeeds == null || customerNeeds.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"section\">");
        html.append("<h2>Your Insurance Needs</h2>");
        html.append("<div class=\"needs-list\">");

        customerNeeds.forEach((key, value) -> {
            if (value != null && !value.toString().isEmpty()) {
                String displayKey = formatKey(key);
                html.append("<div class=\"need-item\">");
                html.append("<strong>").append(displayKey).append(":</strong> ");
                html.append(formatValue(value));
                html.append("</div>");
            }
        });

        html.append("</div>");
        html.append("</div>");

        return html.toString();
    }

    private String generateProductComparison(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return "<div class=\"section\"><h2>Recommended Products</h2><p>No products selected.</p></div>";
        }

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"section\">");
        html.append("<h2>Recommended Products</h2>");

        // Generate product cards
        for (Product product : products) {
            html.append("<div class=\"product-card\">");
            html.append("<h3>").append(escapeHtml(product.getName())).append("</h3>");
            html.append("<p class=\"insurer\">").append(escapeHtml(product.getInsurer())).append("</p>");

            if (product.getPlanType() != null) {
                html.append("<p><strong>Plan Type:</strong> ").append(escapeHtml(product.getPlanType())).append("</p>");
            }

            html.append("<p><strong>Category:</strong> ").append(escapeHtml(product.getCategory().getName())).append("</p>");

            if (product.getDetailsJson() != null && !product.getDetailsJson().isEmpty()) {
                html.append("<div style=\"margin-top: 15px;\">");
                html.append("<strong>Key Features:</strong>");
                html.append("<ul>");
                product.getDetailsJson().forEach((key, value) -> {
                    html.append("<li><strong>").append(formatKey(key)).append(":</strong> ");
                    html.append(formatValue(value)).append("</li>");
                });
                html.append("</ul>");
                html.append("</div>");
            }

            html.append("</div>");
        }

        html.append("</div>");

        return html.toString();
    }

    private String generateAdditionalNotes(String additionalNotes) {
        if (additionalNotes == null || additionalNotes.isEmpty()) {
            return "";
        }

        return String.format("""
            <div class="section">
                <h2>Additional Information</h2>
                <p>%s</p>
            </div>
            """, escapeHtml(additionalNotes));
    }

    private String generateFooter(User agent) {
        return String.format("""
            <div class="footer">
                <div class="agent-info">
                    <p><strong>Your Insurance Agent</strong></p>
                    <p>%s</p>
                    <p>%s</p>
                </div>
                <p>This proposal is valid for 30 days from the date of issue.</p>
                <p>Please contact your agent for any questions or clarifications.</p>
            </div>
            """, agent.getName(), agent.getEmail());
    }

    private String formatKey(String key) {
        // Convert camelCase or snake_case to Title Case
        return key.replaceAll("([A-Z])", " $1")
                  .replaceAll("_", " ")
                  .replaceAll("\\s+", " ")
                  .trim()
                  .substring(0, 1).toUpperCase() +
               key.replaceAll("([A-Z])", " $1")
                  .replaceAll("_", " ")
                  .replaceAll("\\s+", " ")
                  .trim()
                  .substring(1);
    }

    private String formatValue(Object value) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return String.join(", ", list.stream().map(Object::toString).toList());
        }
        return escapeHtml(value.toString());
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
