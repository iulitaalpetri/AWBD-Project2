package com.awbd.bookstore.repositories;

import com.awbd.bookstore.models.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    //dupa cod
    Sale findBySaleCode(String saleCode);

    // get all active sales
    @Query("SELECT s FROM Sale s WHERE s.isActive = true")
    List<Sale> findAllActiveSales();

    // verificare daca promotia e activa
    boolean existsByIsActiveAndSaleCode(Boolean isActive, String saleCode);

    // promotie dupa categorie
    @Query("SELECT s FROM Sale s JOIN s.categories c WHERE c.id = :categoryId AND s.isActive = true")
    List<Sale> findActiveSalesByCategoryId(@Param("categoryId") Long categoryId);

    // get sale by id
    @Query("SELECT s FROM Sale s WHERE s.id = :id")
    Sale findSaleById(@Param("id") Long id);
}