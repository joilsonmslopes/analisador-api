package com.joilson.analisador_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.joilson.analisador_api.dto.ProductResponseDto;
import com.joilson.analisador_api.dto.RatingDto;
import com.joilson.analisador_api.dto.scrape.ScrapedProductDto;
import com.joilson.analisador_api.enums.ProductSortOption;
import com.joilson.analisador_api.enums.SentimentEnum;
import com.joilson.analisador_api.exception.GlobalExceptionHandler;
import com.joilson.analisador_api.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {
    @Mock ProductService service;
    @InjectMocks ProductController controller;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void analyzeRawDeserializesTheExtensionPayloadAndReturnsCreated() throws Exception {
        when(service.createFromRawData(any())).thenReturn(response());
        mvc.perform(post("/api/products/analyze-raw").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Mouse","currentPrice":19.90,"originalLink":"https://produto.mercadolivre.com.br/MLB-123","ratings":["Bom"]}
                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.name").value("Mouse"))
                .andExpect(jsonPath("$.currentPrice").value(19.90))
                .andExpect(jsonPath("$.originalLink").value("https://produto.mercadolivre.com.br/MLB-123"))
                .andExpect(jsonPath("$.ratings[0].originalText").value("Bom"))
                .andExpect(jsonPath("$.ratings[0].sentiment").value("POSITIVO"));
        var input = ArgumentCaptor.forClass(ScrapedProductDto.class);
        verify(service).createFromRawData(input.capture());
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.junit.jupiter.api.Assertions.assertEquals("Mouse", input.getValue().getName()),
                () -> org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("19.90"), input.getValue().getCurrentPrice()),
                () -> org.junit.jupiter.api.Assertions.assertEquals(response().getOriginalLink(), input.getValue().getOriginalLink()),
                () -> org.junit.jupiter.api.Assertions.assertEquals(List.of("Bom"), input.getValue().getRatings()));
        verifyNoMoreInteractions(service);
    }

    @Test
    void allUsesDefaultPaginationAndSort() throws Exception {
        when(service.getAllProducts(0, 10, ProductSortOption.NAME_ASC))
                .thenReturn(new PageImpl<>(List.of(response()), PageRequest.of(0, 10), 1));
        mvc.perform(get("/api/products/all"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(42))
                .andExpect(jsonPath("$.content[0].ratings[0].sentiment").value("POSITIVO"))
                .andExpect(jsonPath("$.number").value(0)).andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.totalPages").value(1));
        verify(service).getAllProducts(0, 10, ProductSortOption.NAME_ASC);
    }

    @Test
    void allForwardsExplicitPaginationAndReturnsEmptyPage() throws Exception {
        when(service.getAllProducts(2, 5, ProductSortOption.PRICE_DESC))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 5), 0));
        mvc.perform(get("/api/products/all").param("page", "2").param("size", "5").param("sort", "PRICE_DESC"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.number").value(2)).andExpect(jsonPath("$.size").value(5));
        verify(service).getAllProducts(2, 5, ProductSortOption.PRICE_DESC);
    }

    @Test
    void argumentFailureReturnsProblemDetail() throws Exception {
        when(service.createFromRawData(any())).thenThrow(new IllegalArgumentException("Entrada inválida"));
        mvc.perform(post("/api/products/analyze-raw").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.detail").value("Entrada inválida"));
    }

    @Test
    void repositoryFailureIsHiddenBehindGenericProblemDetail() throws Exception {
        when(service.createFromRawData(any())).thenThrow(new RuntimeException("private database detail"));
        mvc.perform(post("/api/products/analyze-raw").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Ocorreu um erro interno no servidor."));
    }

    private ProductResponseDto response() {
        var result = new ProductResponseDto();
        result.setId(42L); result.setName("Mouse"); result.setCurrentPrice(new BigDecimal("19.90"));
        result.setOriginalLink("https://produto.mercadolivre.com.br/MLB-123");
        var rating = new RatingDto(); rating.setOriginalText("Bom"); rating.setSentiment(SentimentEnum.POSITIVO);
        result.setRatings(List.of(rating));
        return result;
    }
}
