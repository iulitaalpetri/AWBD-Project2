package com.awbd.bookstore.unitTests;

import com.awbd.bookstore.DTOs.SaleDTO;
import com.awbd.bookstore.controllers.SaleController;
import com.awbd.bookstore.mappers.SaleMapper;
import com.awbd.bookstore.models.Sale;
import com.awbd.bookstore.services.SaleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleControllerTest {

    @Mock
    private SaleService saleService;

    @Mock
    private SaleMapper saleMapper;

    @InjectMocks
    private SaleController saleController;

    private Sale testSale;
    private SaleDTO testSaleDTO;
    private List<Long> categoryIds;

    @BeforeEach
    void setUp() {
        categoryIds = Arrays.asList(1L, 2L);

        testSale = new Sale();
        testSale.setId(1L);
        testSale.setSaleCode("SALE2024");
        testSale.setDiscountPercentage(20.0);
        testSale.setStartDate(LocalDate.of(2024, 1, 1));
        testSale.setEndDate(LocalDate.of(2024, 12, 31));
        testSale.setDescription("Winter Sale");
        testSale.setIsActive(true);

        testSaleDTO = new SaleDTO();
        testSaleDTO.setId(1L);
        testSaleDTO.setSaleCode("SALE2024");
        testSaleDTO.setDiscountPercentage(20.0);
        testSaleDTO.setStartDate(LocalDate.of(2024, 1, 1));
        testSaleDTO.setEndDate(LocalDate.of(2024, 12, 31));
        testSaleDTO.setDescription("Winter Sale");
        testSaleDTO.setIsActive(true);
        testSaleDTO.setCategoryIds(categoryIds);
    }

    @Test
    void createSale_Success() {
        Sale createdSale = new Sale();
        createdSale.setId(1L);
        createdSale.setSaleCode("SALE2024");

        Sale savedSaleWithStatus = new Sale();
        savedSaleWithStatus.setId(1L);
        savedSaleWithStatus.setSaleCode("SALE2024");
        savedSaleWithStatus.setIsActive(true);

        when(saleMapper.toEntity(testSaleDTO)).thenReturn(testSale);
        when(saleService.create(testSale, categoryIds)).thenReturn(createdSale);
        when(saleService.getByIdWithStatusCheck(1L)).thenReturn(savedSaleWithStatus);
        when(saleMapper.toDto(savedSaleWithStatus)).thenReturn(testSaleDTO);

        ResponseEntity<SaleDTO> result = saleController.createSale(testSaleDTO);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(testSaleDTO, result.getBody());
        assertEquals(URI.create("/api/sales/1"), result.getHeaders().getLocation());
        verify(saleMapper, times(1)).toEntity(testSaleDTO);
        verify(saleService, times(1)).create(testSale, categoryIds);
        verify(saleService, times(1)).getByIdWithStatusCheck(1L);
        verify(saleMapper, times(1)).toDto(savedSaleWithStatus);
    }

    @Test
    void getSaleById_Success() {
        Sale saleWithStatus = new Sale();
        saleWithStatus.setId(1L);
        saleWithStatus.setIsActive(true);

        when(saleService.getById(1L)).thenReturn(testSale);
        when(saleService.getByIdWithStatusCheck(1L)).thenReturn(saleWithStatus);
        when(saleMapper.toDto(saleWithStatus)).thenReturn(testSaleDTO);

        ResponseEntity<SaleDTO> result = saleController.getSaleById(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(testSaleDTO, result.getBody());
        verify(saleService, times(1)).getById(1L);
        verify(saleService, times(1)).getByIdWithStatusCheck(1L);
        verify(saleMapper, times(1)).toDto(saleWithStatus);
    }

    @Test
    void getAllSales_Success() {
        List<Sale> sales = Arrays.asList(testSale);
        List<Sale> salesWithStatus = Arrays.asList(testSale);
        List<SaleDTO> saleDTOs = Arrays.asList(testSaleDTO);

        when(saleService.getAll()).thenReturn(sales);
        when(saleService.getAllWithStatusCheck(sales)).thenReturn(salesWithStatus);
        when(saleMapper.toDtoList(salesWithStatus)).thenReturn(saleDTOs);

        List<SaleDTO> result = saleController.getAllSales();

        assertEquals(saleDTOs, result);
        assertEquals(1, result.size());
        assertEquals("SALE2024", result.get(0).getSaleCode());
        verify(saleService, times(1)).getAll();
        verify(saleService, times(1)).getAllWithStatusCheck(sales);
        verify(saleMapper, times(1)).toDtoList(salesWithStatus);
    }

    @Test
    void getAllSales_EmptyList() {
        List<Sale> emptySales = Arrays.asList();
        List<Sale> emptySalesWithStatus = Arrays.asList();
        List<SaleDTO> emptySaleDTOs = Arrays.asList();

        when(saleService.getAll()).thenReturn(emptySales);
        when(saleService.getAllWithStatusCheck(emptySales)).thenReturn(emptySalesWithStatus);
        when(saleMapper.toDtoList(emptySalesWithStatus)).thenReturn(emptySaleDTOs);

        List<SaleDTO> result = saleController.getAllSales();

        assertEquals(emptySaleDTOs, result);
        assertEquals(0, result.size());
        verify(saleService, times(1)).getAll();
        verify(saleService, times(1)).getAllWithStatusCheck(emptySales);
        verify(saleMapper, times(1)).toDtoList(emptySalesWithStatus);
    }

    @Test
    void getActiveSales_Success() {
        List<Sale> activeSales = Arrays.asList(testSale);
        List<Sale> activeSalesWithStatus = Arrays.asList(testSale);
        List<SaleDTO> activeSaleDTOs = Arrays.asList(testSaleDTO);

        when(saleService.getAllActiveSales()).thenReturn(activeSales);
        when(saleService.getAllWithStatusCheck(activeSales)).thenReturn(activeSalesWithStatus);
        when(saleMapper.toDtoList(activeSalesWithStatus)).thenReturn(activeSaleDTOs);

        List<SaleDTO> result = saleController.getActiveSales();

        assertEquals(activeSaleDTOs, result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
        verify(saleService, times(1)).getAllActiveSales();
        verify(saleService, times(1)).getAllWithStatusCheck(activeSales);
        verify(saleMapper, times(1)).toDtoList(activeSalesWithStatus);
    }

    @Test
    void getActiveSales_EmptyList() {
        List<Sale> emptyActiveSales = Arrays.asList();
        List<Sale> emptyActiveSalesWithStatus = Arrays.asList();
        List<SaleDTO> emptyActiveSaleDTOs = Arrays.asList();

        when(saleService.getAllActiveSales()).thenReturn(emptyActiveSales);
        when(saleService.getAllWithStatusCheck(emptyActiveSales)).thenReturn(emptyActiveSalesWithStatus);
        when(saleMapper.toDtoList(emptyActiveSalesWithStatus)).thenReturn(emptyActiveSaleDTOs);

        List<SaleDTO> result = saleController.getActiveSales();

        assertEquals(emptyActiveSaleDTOs, result);
        assertEquals(0, result.size());
        verify(saleService, times(1)).getAllActiveSales();
        verify(saleService, times(1)).getAllWithStatusCheck(emptyActiveSales);
        verify(saleMapper, times(1)).toDtoList(emptyActiveSalesWithStatus);
    }}