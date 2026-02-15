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

    @Query("SELECT p FROM Product p WHERE " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:insurer IS NULL OR LOWER(p.insurer) = LOWER(:insurer)) AND " +
           "(:planType IS NULL OR LOWER(p.planType) = LOWER(:planType))")
    List<Product> findByFilters(@Param("categoryId") Long categoryId,
                                @Param("insurer") String insurer,
                                @Param("planType") String planType);
}
