package com.insurance.products.repository;

import com.insurance.common.entity.ProductDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductDocumentRepository extends JpaRepository<ProductDocument, Long> {

    List<ProductDocument> findByProductId(Long productId);

    List<ProductDocument> findByCategoryId(Long categoryId);

    @Query("SELECT pd FROM ProductDocument pd WHERE pd.product.id = :productId ORDER BY pd.createdAt DESC")
    List<ProductDocument> findByProductIdOrderByCreatedAtDesc(@Param("productId") Long productId);

    @Query("SELECT pd FROM ProductDocument pd WHERE pd.category.id = :categoryId ORDER BY pd.createdAt DESC")
    List<ProductDocument> findByCategoryIdOrderByCreatedAtDesc(@Param("categoryId") Long categoryId);

    @Query("SELECT pd FROM ProductDocument pd WHERE " +
           "pd.product IS NULL AND pd.category.id = :categoryId")
    List<ProductDocument> findCategoryDocumentsOnly(@Param("categoryId") Long categoryId);
}
