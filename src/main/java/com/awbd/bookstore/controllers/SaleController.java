package com.awbd.bookstore.controllers;

import com.awbd.bookstore.DTOs.SaleDTO;
import com.awbd.bookstore.models.Sale;
import com.awbd.bookstore.services.SaleService;
import com.awbd.bookstore.mappers.SaleMapper;
import com.awbd.bookstore.annotations.RequireAdmin;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private SaleService saleService;
    private SaleMapper saleMapper;
    private static final Logger logger = LoggerFactory.getLogger(SaleController.class);

    public SaleController(SaleService saleService, SaleMapper saleMapper) {
        this.saleService = saleService;
        this.saleMapper = saleMapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SaleDTO> createSale(
            @RequestBody
            @Valid
            SaleDTO saleDTO) {
        Sale sale = saleMapper.toEntity(saleDTO);
        Sale saved = saleService.create(sale, saleDTO.getCategoryIds());
        saved = saleService.getByIdWithStatusCheck(saved.getId());
        logger.info("Created sale with ID: {}", saved.getId());

        return ResponseEntity.created(URI.create("/api/sales/" + saved.getId()))
                .body(saleMapper.toDto(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleDTO> getSaleById(@PathVariable Long id) {
        Sale sale = saleService.getById(id);
        sale = saleService.getByIdWithStatusCheck(sale.getId());
        logger.info("Retrieved sale with ID: {}", id);
        return ResponseEntity.ok(saleMapper.toDto(sale));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<SaleDTO> getAllSales() {
        List<Sale> sales = saleService.getAll();
        sales = saleService.getAllWithStatusCheck(sales);
        logger.info("Retrieved {} sales", sales.size());
        return saleMapper.toDtoList(sales);
    }

    @GetMapping("/active")
    public List<SaleDTO> getActiveSales() {
        List<Sale> sales = saleService.getAllActiveSalesWithStatusCheck();
        logger.info("Retrieved {} active sales", sales.size());
        return saleMapper.toDtoList(sales);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SaleDTO> updateSale(
            @PathVariable
            Long id,

            @RequestBody
            @Valid
            SaleDTO saleDTO) {

        if (saleDTO.getId() != null && !id.equals(saleDTO.getId())) {
            logger.warn("ID mismatch: path ID {} doesn't match body ID {}", id, saleDTO.getId());
            throw new RuntimeException("Id from path does not match with id from request");
        }

        Sale sale = saleMapper.toEntity(saleDTO);
        Sale updated = saleService.update(id, sale, saleDTO.getCategoryIds());

        logger.info("Updated sale with ID: {}", id);

        return ResponseEntity.ok(saleMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSale(@PathVariable Long id) {
        saleService.delete(id);
        logger.info("Deleted sale with ID: {}", id);
        return ResponseEntity.noContent().build();
    }
}