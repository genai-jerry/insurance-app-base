package com.insurance.rag.service;

import com.insurance.common.entity.Product;
import com.insurance.common.entity.VectorEmbedding;
import com.insurance.common.entity.VoiceSession;
import com.insurance.products.repository.ProductRepository;
import com.insurance.voice.repository.VoiceSessionRepository;
import com.insurance.rag.dto.ProductRecommendationRequest;
import com.insurance.rag.dto.ProductRecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final EmbeddingService embeddingService;
    private final ProductRepository productRepository;
    private final VoiceSessionRepository voiceSessionRepository;
    private final ChatClient.Builder chatClientBuilder;

    /**
     * Generate product recommendations using RAG pipeline
     */
    @Transactional
    public ProductRecommendationResponse recommendProducts(ProductRecommendationRequest request) {
        log.info("Generating product recommendations for query: {}", request.getQuery());

        // Get customer needs from various sources
        Map<String, Object> customerNeeds = gatherCustomerNeeds(request);

        // Build search query
        String searchQuery = buildSearchQuery(request.getQuery(), customerNeeds);

        // Retrieve relevant products using vector search
        int maxResults = request.getMaxResults() != null ? request.getMaxResults() : 5;
        List<VectorEmbedding> similarEmbeddings = embeddingService.findSimilarByType(
            searchQuery,
            VectorEmbedding.EntityType.PRODUCT,
            maxResults * 2 // Get more candidates for ranking
        );

        // Get unique products from embeddings
        Set<Long> productIds = similarEmbeddings.stream()
            .map(VectorEmbedding::getEntityId)
            .collect(Collectors.toSet());

        List<Product> candidateProducts = productRepository.findAllById(productIds);

        // Rank products based on needs match
        List<ProductRecommendationResponse.RecommendedProduct> rankedProducts = rankProducts(
            candidateProducts,
            customerNeeds,
            maxResults
        );

        // Generate narrative explanation
        String narrative = generateNarrative(rankedProducts, customerNeeds);

        // Store recommendations in voice session if applicable
        if (request.getVoiceSessionId() != null) {
            storeRecommendationsInSession(request.getVoiceSessionId(), rankedProducts);
        }

        return ProductRecommendationResponse.builder()
            .narrative(narrative)
            .products(rankedProducts)
            .matchedNeeds(customerNeeds)
            .build();
    }

    /**
     * Gather customer needs from various sources
     */
    private Map<String, Object> gatherCustomerNeeds(ProductRecommendationRequest request) {
        Map<String, Object> needs = new HashMap<>();

        // Add needs from request
        if (request.getCustomerNeeds() != null) {
            needs.putAll(request.getCustomerNeeds());
        }

        // Get needs from voice session if available
        if (request.getVoiceSessionId() != null) {
            Optional<VoiceSession> sessionOpt = voiceSessionRepository.findById(request.getVoiceSessionId());
            if (sessionOpt.isPresent() && sessionOpt.get().getExtractedNeedsJson() != null) {
                needs.putAll(sessionOpt.get().getExtractedNeedsJson());
            }
        }

        return needs;
    }

    /**
     * Build search query from customer needs
     */
    private String buildSearchQuery(String originalQuery, Map<String, Object> customerNeeds) {
        StringBuilder query = new StringBuilder(originalQuery);

        if (customerNeeds.containsKey("insuranceTypes")) {
            Object types = customerNeeds.get("insuranceTypes");
            if (types instanceof List) {
                query.append(" ").append(String.join(" ", (List<String>) types));
            }
        }

        if (customerNeeds.containsKey("coverageAmount")) {
            query.append(" coverage: ").append(customerNeeds.get("coverageAmount"));
        }

        if (customerNeeds.containsKey("familySituation")) {
            query.append(" ").append(customerNeeds.get("familySituation"));
        }

        return query.toString();
    }

    /**
     * Rank products based on needs match
     */
    private List<ProductRecommendationResponse.RecommendedProduct> rankProducts(
        List<Product> products,
        Map<String, Object> customerNeeds,
        int limit
    ) {
        List<ProductRecommendationResponse.RecommendedProduct> ranked = new ArrayList<>();

        for (Product product : products) {
            double score = calculateRelevanceScore(product, customerNeeds);
            String reasoning = generateReasoning(product, customerNeeds);

            ProductRecommendationResponse.RecommendedProduct recommended =
                ProductRecommendationResponse.RecommendedProduct.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .insurer(product.getInsurer())
                    .planType(product.getPlanType())
                    .relevanceScore(score)
                    .reasoning(reasoning)
                    .details(product.getDetailsJson())
                    .build();

            ranked.add(recommended);
        }

        // Sort by relevance score descending
        ranked.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));

        return ranked.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Calculate relevance score for a product
     */
    private double calculateRelevanceScore(Product product, Map<String, Object> customerNeeds) {
        double score = 0.5; // Base score

        // Check insurance type match
        if (customerNeeds.containsKey("insuranceTypes")) {
            Object types = customerNeeds.get("insuranceTypes");
            if (types instanceof List) {
                List<String> neededTypes = (List<String>) types;
                String categoryName = product.getCategory().getName().toLowerCase();

                for (String type : neededTypes) {
                    if (categoryName.contains(type.toLowerCase())) {
                        score += 0.3;
                        break;
                    }
                }
            }
        }

        // Check tags match
        if (product.getTags() != null && customerNeeds.containsKey("concerns")) {
            Object concerns = customerNeeds.get("concerns");
            if (concerns instanceof List) {
                List<String> customerConcerns = (List<String>) concerns;
                for (String tag : product.getTags()) {
                    for (String concern : customerConcerns) {
                        if (tag.toLowerCase().contains(concern.toLowerCase()) ||
                            concern.toLowerCase().contains(tag.toLowerCase())) {
                            score += 0.1;
                        }
                    }
                }
            }
        }

        return Math.min(score, 1.0); // Cap at 1.0
    }

    /**
     * Generate reasoning for recommendation
     */
    private String generateReasoning(Product product, Map<String, Object> customerNeeds) {
        StringBuilder reasoning = new StringBuilder();

        reasoning.append("This ").append(product.getCategory().getName())
                 .append(" product from ").append(product.getInsurer())
                 .append(" matches your needs");

        if (customerNeeds.containsKey("insuranceTypes")) {
            reasoning.append(" for ").append(customerNeeds.get("insuranceTypes"));
        }

        if (customerNeeds.containsKey("budget")) {
            reasoning.append(" within your budget of ").append(customerNeeds.get("budget"));
        }

        return reasoning.toString();
    }

    /**
     * Generate narrative explanation using AI
     */
    private String generateNarrative(
        List<ProductRecommendationResponse.RecommendedProduct> products,
        Map<String, Object> customerNeeds
    ) {
        if (products.isEmpty()) {
            return "No suitable products found matching your requirements.";
        }

        try {
            String prompt = buildNarrativePrompt(products, customerNeeds);

            ChatClient chatClient = chatClientBuilder.build();

            String narrative = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

            return narrative;

        } catch (Exception e) {
            log.error("Failed to generate narrative", e);
            return buildFallbackNarrative(products);
        }
    }

    private String buildNarrativePrompt(
        List<ProductRecommendationResponse.RecommendedProduct> products,
        Map<String, Object> customerNeeds
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an insurance advisor. Create a brief, professional narrative explaining ");
        prompt.append("why these products are recommended for the customer.\n\n");

        prompt.append("Customer Needs:\n");
        customerNeeds.forEach((key, value) -> prompt.append("- ").append(key).append(": ").append(value).append("\n"));

        prompt.append("\nRecommended Products:\n");
        for (int i = 0; i < products.size(); i++) {
            ProductRecommendationResponse.RecommendedProduct p = products.get(i);
            prompt.append(i + 1).append(". ").append(p.getProductName())
                  .append(" by ").append(p.getInsurer())
                  .append(" (Score: ").append(String.format("%.2f", p.getRelevanceScore())).append(")\n");
        }

        prompt.append("\nProvide a concise 2-3 sentence explanation of why these products match the customer's needs.");

        return prompt.toString();
    }

    private String buildFallbackNarrative(List<ProductRecommendationResponse.RecommendedProduct> products) {
        StringBuilder narrative = new StringBuilder();
        narrative.append("Based on your requirements, we recommend the following products: ");

        for (int i = 0; i < Math.min(3, products.size()); i++) {
            if (i > 0) narrative.append(", ");
            narrative.append(products.get(i).getProductName());
        }

        narrative.append(". These products best match your needs and budget.");
        return narrative.toString();
    }

    /**
     * Store recommendations in voice session
     */
    @Transactional
    private void storeRecommendationsInSession(
        Long sessionId,
        List<ProductRecommendationResponse.RecommendedProduct> products
    ) {
        Optional<VoiceSession> sessionOpt = voiceSessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            VoiceSession session = sessionOpt.get();

            Map<String, Object> recommendations = new HashMap<>();
            recommendations.put("products", products);
            recommendations.put("generatedAt", java.time.LocalDateTime.now().toString());

            session.setRecommendationsJson(recommendations);
            voiceSessionRepository.save(session);

            log.info("Stored recommendations in voice session {}", sessionId);
        }
    }
}
