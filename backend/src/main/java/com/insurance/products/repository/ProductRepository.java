package com.insurance.products.repository;

import com.insurance.common.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByInsurer(String insurer);

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.insurer = :insurer")
    List<Product> findByCategoryIdAndInsurer(@Param("categoryId") Long categoryId, @Param("insurer") String insurer);

    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.insurer) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.planType) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Product> searchProducts(@Param("searchTerm") String searchTerm);

    /**
     * Lists products narrowed by every supplied filter; a null parameter skips its
     * predicate. The name term matches by case-insensitive substring and is bound as
     * data, never interpolated into the query structure. Ordered by id ascending so
     * repeated identical requests return matches in the same, stable order.
     */
    @Query("SELECT p FROM Product p WHERE " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:insurer IS NULL OR LOWER(p.insurer) = LOWER(CAST(:insurer AS string))) AND " +
           "(:planType IS NULL OR LOWER(p.planType) = LOWER(CAST(:planType AS string))) AND " +
           "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
           "ORDER BY p.id ASC")
    List<Product> findByFilters(@Param("categoryId") Long categoryId,
                                @Param("insurer") String insurer,
                                @Param("planType") String planType,
                                @Param("name") String name);
}
