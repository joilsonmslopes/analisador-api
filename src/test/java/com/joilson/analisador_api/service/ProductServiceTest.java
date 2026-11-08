package com.joilson.analisador_api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.joilson.analisador_api.dto.scrape.ScrapedProductDto;
import com.joilson.analisador_api.enums.ProductSortOption;
import com.joilson.analisador_api.enums.SentimentEnum;
import com.joilson.analisador_api.mapper.ProductMapper;
import com.joilson.analisador_api.model.Product;
import com.joilson.analisador_api.repository.ProductRepository;


@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock ProductRepository repository;

    @Mock AiService ai;
    ProductService service;

    @BeforeEach
    void setup() { service = new ProductService(repository, ai, new ProductMapper(), new com.joilson.analisador_api.marketplace.MarketplaceProcessorRegistry(List.of(new com.joilson.analisador_api.marketplace.MercadoLivreProcessor()))); }

    @Test
    void savesTheAggregateWithBidirectionalRatingsAndPreservesInputOrder() {
        when(ai.analyzeSentiment("Bom")).thenReturn(SentimentEnum.POSITIVO);
        when(ai.analyzeSentiment("Ruim")).thenReturn(SentimentEnum.NEGATIVO);
        when(ai.analyzeSentiment("Normal")).thenReturn(SentimentEnum.NEUTRO);
        when(repository.save(any(Product.class))).thenAnswer(call -> {
            Product product = call.getArgument(0);
            assertEquals("Mouse", product.getName());
            assertEquals(new BigDecimal("19.90"), product.getCurrentPrice());
            assertEquals("https://produto.mercadolivre.com.br/MLB-123", product.getOriginalLink());
            assertEquals(List.of("Bom", "Ruim", "Normal"), product.getRatings().stream().map(r -> r.getOriginalText()).toList());
            assertEquals(List.of("Positivo", "Negativo", "Neutro"), product.getRatings().stream().map(r -> r.getSentiment()).toList());
            product.getRatings().forEach(rating -> assertSame(product, rating.getProduct()));
            product.setId(42L);
            return product;
        });
        var result = service.createFromRawData(input(List.of("Bom", "Ruim", "Normal")));
        assertEquals(42L, result.getId());
        assertEquals(List.of(SentimentEnum.POSITIVO, SentimentEnum.NEGATIVO, SentimentEnum.NEUTRO),
                result.getRatings().stream().map(r -> r.getSentiment()).toList());
        verify(ai).analyzeSentiment("Bom"); verify(ai).analyzeSentiment("Ruim"); verify(ai).analyzeSentiment("Normal");
        verify(repository).save(any(Product.class));

    }

    @Test
    void emptyRatingsAreSavedWithoutCallingAi() {
        saveUnchanged();
        assertTrue(service.createFromRawData(input(List.of())).getRatings().isEmpty());
        verify(repository).save(any(Product.class));
        verifyNoInteractions(ai);
    }

    @Test
    void failedAiTaskCurrentlyDropsOnlyItsCommentAndStillSaves() {
        when(ai.analyzeSentiment("Falha")).thenThrow(new RuntimeException("simulated failure"));
        when(ai.analyzeSentiment("Bom")).thenReturn(SentimentEnum.POSITIVO);
        saveUnchanged();
        var result = service.createFromRawData(input(List.of("Falha", "Bom")));
        assertEquals(1, result.getRatings().size());
        assertEquals("Bom", result.getRatings().getFirst().getOriginalText());
        verify(repository).save(any(Product.class));

    }

    @Test
    void allAiTasksFailingCurrentlySavesProductWithoutRatings() {
        when(ai.analyzeSentiment("Falha")).thenThrow(new RuntimeException("simulated failure"));
        saveUnchanged();
        assertTrue(service.createFromRawData(input(List.of("Falha"))).getRatings().isEmpty());
        verify(repository).save(any(Product.class));
    }

    @Test
    void nullRatingsAreNormalizedToEmpty() {
        saveUnchanged();
        assertTrue(service.createFromRawData(input(null)).getRatings().isEmpty());
        verifyNoInteractions(ai);
    }

    @Test
    void invalidMarketplaceStopsBeforeAiAndPersistence() {
        var dto = input(List.of("Bom"));
        dto.setMarketplace("unsupported");
        assertThrows(IllegalArgumentException.class, () -> service.createFromRawData(dto));
        verifyNoInteractions(repository, ai);
    }

    @Test
    void persistenceFailurePropagates() {
        var failure = new IllegalStateException("simulated save failure");
        when(repository.save(any(Product.class))).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.createFromRawData(input(List.of()))));
        verifyNoInteractions(ai);
    }

    @Test
    void listUsesDefaultSortAndMapsRepositoryPage() {
        var pageable = PageRequest.of(0, 10, ProductSortOption.NAME_ASC.getSort());
        var product = new ProductMapper().toEntity(input(List.of())); product.setId(5L);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(product), pageable, 1));
        var result = service.getAllProducts(0, 10, null);
        assertEquals(1, result.getTotalElements()); assertEquals(5L, result.getContent().getFirst().getId());
        verify(repository).findAll(pageable); verifyNoInteractions(ai);
    }

    @Test
    void listUsesRequestedSortAndRejectsInvalidPagination() {
        var pageable = PageRequest.of(1, 3, ProductSortOption.PRICE_DESC.getSort());
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));
        assertTrue(service.getAllProducts(1, 3, ProductSortOption.PRICE_DESC).isEmpty());
        verify(repository).findAll(pageable);
        assertThrows(IllegalArgumentException.class, () -> service.getAllProducts(-1, 3, null));
        assertThrows(IllegalArgumentException.class, () -> service.getAllProducts(0, 0, null));
        verifyNoMoreInteractions(repository);
    }

    private void saveUnchanged() {
        when(repository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));
    }

    private ScrapedProductDto input(List<String> ratings) {
        var dto = new ScrapedProductDto(); dto.setName("Mouse"); dto.setCurrentPrice(new BigDecimal("19.90"));
        dto.setOriginalLink("https://produto.mercadolivre.com.br/MLB-123"); dto.setRatings(ratings);
        return dto;
    }
}
