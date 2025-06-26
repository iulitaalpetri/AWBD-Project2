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
import java.util.stream.Collectors;

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
        logger.info("Creating sale '{}' with {}% discount for {} categories",
                sale.getSaleCode(), sale.getDiscountPercentage(),
                categoryIds != null ? categoryIds.size() : 0);

        if (categoryIds != null && !categoryIds.isEmpty()) {
            logger.debug("Processing {} category IDs: {}", categoryIds.size(), categoryIds);

            List<Category> categories = categoryRepository.findAllById(categoryIds);

            if (categories.size() != categoryIds.size()) {
                logger.warn("Sale creation failed - some categories not found. Expected: {}, Found: {}",
                        categoryIds.size(), categories.size());
                throw new CategoryNotFoundException("One or more categories not found");
            }

            List<String> categoryNames = categories.stream()
                    .map(Category::getName)
                    .collect(Collectors.toList());
            logger.debug("Categories found for sale: {}", categoryNames);

            sale.setCategories(categories);
        } else {
            logger.debug("No categories specified for sale '{}'", sale.getSaleCode());
        }

        Sale savedSale = saleRepository.save(sale);
        logger.info("Sale successfully created - ID: {}, Code: '{}', Discount: {}%, Active: {}, Categories: {}",
                savedSale.getId(), savedSale.getSaleCode(), savedSale.getDiscountPercentage(),
                savedSale.getIsActive(), savedSale.getCategories().size());

        return savedSale;
    }

    public Sale getById(Long id) {
        logger.debug("Finding sale by ID: {}", id);

        return saleRepository.findById(id)
                .map(sale -> {
                    logger.debug("Sale found - ID: {}, Code: '{}', Active: {}",
                            sale.getId(), sale.getSaleCode(), sale.getIsActive());
                    return sale;
                })
                .orElseThrow(() -> {
                    logger.warn("Sale not found with ID: {}", id);
                    return new SaleNotFoundException("Sale with ID " + id + " not found");
                });
    }

    public Sale getByIdWithStatusCheck(Long id) {
        logger.debug("Finding sale by ID with status check: {}", id);

        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Sale not found with ID: {}", id);
                    return new SaleNotFoundException("Sale with ID " + id + " not found");
                });

        boolean wasActive = sale.getIsActive();

        if (sale.updateStatusIfNeeded()) {
            sale = saleRepository.save(sale);
            logger.info("Sale '{}' (ID: {}) status updated: {} -> {}",
                    sale.getSaleCode(), id, wasActive, sale.getIsActive());
        } else {
            logger.debug("Sale '{}' (ID: {}) status unchanged: {}",
                    sale.getSaleCode(), id, sale.getIsActive());
        }

        return sale;
    }

    public List<Sale> getAll() {
        logger.debug("Retrieving all sales");

        List<Sale> sales = saleRepository.findAll();
        logger.info("Retrieved {} sales", sales.size());

        // Log distribution of active vs inactive sales
        if (logger.isDebugEnabled() && !sales.isEmpty()) {
            long activeSales = sales.stream().filter(Sale::getIsActive).count();
            long inactiveSales = sales.size() - activeSales;
            logger.debug("Sales distribution - Active: {}, Inactive: {}", activeSales, inactiveSales);
        }

        return sales;
    }

    public List<Sale> getAllWithStatusCheck(List<Sale> sales) {
        logger.debug("Checking status for {} sales", sales.size());

        boolean hasUpdates = false;
        int updateCount = 0;

        for (Sale sale : sales) {
            boolean wasActive = sale.getIsActive();

            if (sale.updateStatusIfNeeded()) {
                saleRepository.save(sale);
                logger.info("Sale '{}' (ID: {}) status updated: {} -> {}",
                        sale.getSaleCode(), sale.getId(), wasActive, sale.getIsActive());
                hasUpdates = true;
                updateCount++;
            }
        }

        if (hasUpdates) {
            logger.info("Status updates completed for {} out of {} sales", updateCount, sales.size());
        } else {
            logger.debug("No status updates needed for {} sales", sales.size());
        }

        return sales;
    }

    public void updateAllSaleStatuses() {
        logger.info("Starting bulk status update for all sales");

        List<Sale> allSales = saleRepository.findAll();
        int updatedCount = 0;
        int totalSales = allSales.size();

        logger.debug("Processing {} sales for status updates", totalSales);

        for (Sale sale : allSales) {
            boolean wasActive = sale.getIsActive();

            if (sale.updateStatusIfNeeded()) {
                saleRepository.save(sale);
                updatedCount++;
                logger.debug("Sale '{}' (ID: {}) status updated: {} -> {}",
                        sale.getSaleCode(), sale.getId(), wasActive, sale.getIsActive());
            }
        }

        logger.info("Bulk status update completed - Updated: {}/{} sales", updatedCount, totalSales);
    }

    public Sale update(Long id, Sale updatedSale, List<Long> categoryIds) {
        logger.info("Updating sale ID: {} with new discount: {}%", id, updatedSale.getDiscountPercentage());

        return saleRepository.findById(id)
                .map(existingSale -> {
                    String oldCode = existingSale.getSaleCode();
                    Double oldDiscount = existingSale.getDiscountPercentage();
                    Boolean oldActive = existingSale.getIsActive();
                    int oldCategoryCount = existingSale.getCategories().size();

                    existingSale.setDiscountPercentage(updatedSale.getDiscountPercentage());
                    existingSale.setStartDate(updatedSale.getStartDate());
                    existingSale.setEndDate(updatedSale.getEndDate());
                    existingSale.setDescription(updatedSale.getDescription());
                    existingSale.setIsActive(updatedSale.getIsActive());

                    if (categoryIds != null && !categoryIds.isEmpty()) {
                        logger.debug("Updating categories for sale ID: {} - {} category IDs", id, categoryIds.size());

                        List<Category> categories = categoryRepository.findAllById(categoryIds);

                        if (categories.size() != categoryIds.size()) {
                            logger.warn("Sale update failed - some categories not found. Expected: {}, Found: {}",
                                    categoryIds.size(), categories.size());
                            throw new CategoryNotFoundException("One or more categories not found");
                        }

                        existingSale.setCategories(categories);
                        logger.debug("Updated categories for sale '{}': {}",
                                existingSale.getSaleCode(), categories.size());
                    }

                    Sale savedSale = saleRepository.save(existingSale);

                    logger.info("Sale successfully updated - ID: {}, Code: '{}' -> '{}', Discount: {}% -> {}%, Active: {} -> {}, Categories: {} -> {}",
                            id, oldCode, savedSale.getSaleCode(), oldDiscount, savedSale.getDiscountPercentage(),
                            oldActive, savedSale.getIsActive(), oldCategoryCount, savedSale.getCategories().size());

                    return savedSale;
                })
                .orElseThrow(() -> {
                    logger.warn("Sale update failed - sale not found with ID: {}", id);
                    return new SaleNotFoundException("Sale with ID " + id + " not found");
                });
    }

    public void delete(Long id) {
        logger.info("Deleting sale with ID: {}", id);

        if (!saleRepository.existsById(id)) {
            logger.warn("Sale deletion failed - sale not found with ID: {}", id);
            throw new SaleNotFoundException("Sale with ID " + id + " not found");
        }

        // Log sale details before deletion for audit
        Sale saleToDelete = saleRepository.findById(id).orElse(null);
        if (saleToDelete != null) {
            logger.debug("Deleting sale - ID: {}, Code: '{}', Discount: {}%, Categories: {}",
                    id, saleToDelete.getSaleCode(), saleToDelete.getDiscountPercentage(),
                    saleToDelete.getCategories().size());
        }

        saleRepository.deleteById(id);
        logger.info("Sale with ID {} deleted successfully", id);
    }

    public List<Sale> getAllActiveSales() {
        logger.debug("Retrieving all active sales");

        List<Sale> activeSales = saleRepository.findAllActiveSales();
        logger.info("Found {} active sales", activeSales.size());

        // Log details about active sales for debugging
        if (logger.isDebugEnabled() && !activeSales.isEmpty()) {
            activeSales.forEach(sale ->
                    logger.debug("Active sale: '{}' (ID: {}) - {}% discount on {} categories",
                            sale.getSaleCode(), sale.getId(), sale.getDiscountPercentage(),
                            sale.getCategories().size()));
        }

        return activeSales;
    }

    public List<Sale> getAllActiveSalesWithStatusCheck() {
        logger.info("Retrieving active sales with status check");

        // Mai întâi actualizează toate statusurile
        updateAllSaleStatuses();

        // Apoi returnează doar cele active
        List<Sale> activeSales = saleRepository.findAllActiveSales();
        logger.info("Found {} active sales after status update", activeSales.size());

        return activeSales;
    }
}