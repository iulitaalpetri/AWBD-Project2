package com.awbd.bookstore.services;

import com.awbd.bookstore.exceptions.order.SaleNotFoundException;
import com.awbd.bookstore.exceptions.category.CategoryNotFoundException;
import com.awbd.bookstore.models.Category;
import com.awbd.bookstore.models.Sale;
import com.awbd.bookstore.repositories.CategoryRepository;
import com.awbd.bookstore.repositories.SaleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SaleService {

    private static final Logger logger = LoggerFactory.getLogger(SaleService.class);
    private SaleRepository saleRepository;
    private CategoryRepository categoryRepository;

    public SaleService(SaleRepository saleRepository, CategoryRepository categoryRepository) {
        this.saleRepository = saleRepository;
        this.categoryRepository = categoryRepository;
    }

    public Sale create(Sale sale, List<Long> categoryIds) {
        if (categoryIds != null && !categoryIds.isEmpty()) {
            List<Category> categories = categoryRepository.findAllById(categoryIds);

            if (categories.size() != categoryIds.size()) {
                throw new CategoryNotFoundException("One or more categories not found");
            }

            sale.setCategories(categories);
        }

        return saleRepository.save(sale);
    }

    public Sale getById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new SaleNotFoundException("Sale with ID " + id + " not found"));
    }


    public Sale getByIdWithStatusCheck(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new SaleNotFoundException("Sale with ID " + id + " not found"));


        if (sale.updateStatusIfNeeded()) {
            sale = saleRepository.save(sale);
            logger.info("Sale {} status updated to: {}", sale.getSaleCode(), sale.getIsActive());
        }

        return sale;
    }

    public List<Sale> getAll() {
        return saleRepository.findAll();
    }


    public List<Sale> getAllWithStatusCheck(List<Sale> sales) {

        boolean hasUpdates = false;

        for (Sale sale : sales) {
            if (sale.updateStatusIfNeeded()) {
                saleRepository.save(sale);
                logger.info("Sale {} status updated to: {}", sale.getSaleCode(), sale.getIsActive());
                hasUpdates = true;
            }
        }

        if (hasUpdates) {
            logger.info("Status updates completed for multiple sales");
        }

        return sales;
    }


    public void updateAllSaleStatuses() {
        List<Sale> allSales = saleRepository.findAll();
        int updatedCount = 0;

        for (Sale sale : allSales) {
            if (sale.updateStatusIfNeeded()) {
                saleRepository.save(sale);
                updatedCount++;
                logger.debug("Sale {} status updated to: {}", sale.getSaleCode(), sale.getIsActive());
            }
        }

        logger.info("Updated status for {} sales", updatedCount);
    }

    public Sale update(Long id, Sale updatedSale, List<Long> categoryIds) {
        return saleRepository.findById(id)
                .map(existingSale -> {
                    existingSale.setDiscountPercentage(updatedSale.getDiscountPercentage());
                    existingSale.setStartDate(updatedSale.getStartDate());
                    existingSale.setEndDate(updatedSale.getEndDate());
                    existingSale.setDescription(updatedSale.getDescription());
                    existingSale.setIsActive(updatedSale.getIsActive());

                    if (categoryIds != null && !categoryIds.isEmpty()) {
                        List<Category> categories = categoryRepository.findAllById(categoryIds);

                        if (categories.size() != categoryIds.size()) {
                            throw new CategoryNotFoundException("One or more categories not found");
                        }

                        existingSale.setCategories(categories);
                    }

                    return saleRepository.save(existingSale);
                })
                .orElseThrow(() -> new SaleNotFoundException("Sale with ID " + id + " not found"));
    }

    public void delete(Long id) {
        if (!saleRepository.existsById(id)) {
            throw new SaleNotFoundException("Sale with ID " + id + " not found");
        }
        saleRepository.deleteById(id);
    }

    public List<Sale> getAllActiveSales() {
        return saleRepository.findAllActiveSales();
    }


    public List<Sale> getAllActiveSalesWithStatusCheck() {
        // Mai întâi actualizează toate statusurile
        updateAllSaleStatuses();

        // Apoi returnează doar cele active
        return saleRepository.findAllActiveSales();
    }

    
}