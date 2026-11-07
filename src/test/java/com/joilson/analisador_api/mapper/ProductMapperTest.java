package com.joilson.analisador_api.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import com.joilson.analisador_api.dto.scrape.ScrapedProductDto;
import com.joilson.analisador_api.enums.SentimentEnum;
import com.joilson.analisador_api.model.Product;
import com.joilson.analisador_api.model.Rating;

class ProductMapperTest {
    private final ProductMapper mapper = new ProductMapper();

    @Test
    void mapsScalarInputWithoutConstructingRatings() {
        var input = new ScrapedProductDto(); input.setName("Mouse"); input.setCurrentPrice(new BigDecimal("19.90"));
        input.setOriginalLink("https://example.invalid/product"); input.setRatings(List.of("Bom"));
        var product = mapper.toEntity(input);
        assertEquals(input.getName(), product.getName()); assertEquals(input.getCurrentPrice(), product.getCurrentPrice());
        assertEquals(input.getOriginalLink(), product.getOriginalLink()); assertNull(product.getId());
        assertTrue(product.getRatings().isEmpty());
    }

    @Test
    void mapsAggregateToResponseWithoutMutatingRelationships() {
        var product = new Product(); product.setId(42L); product.setName("Mouse");
        product.setCurrentPrice(new BigDecimal("19.90")); product.setOriginalLink("https://example.invalid/product");
        var positive = rating(product, "Bom", "Positivo");
        var unknown = rating(product, "Incerto", "unexpected");
        var missing = rating(product, "Sem sentimento", null);
        product.setRatings(Arrays.asList(positive, unknown, missing, null));
        var result = mapper.toResponseDto(product);
        assertEquals(product.getId(), result.getId()); assertEquals(product.getName(), result.getName());
        assertEquals(product.getCurrentPrice(), result.getCurrentPrice()); assertEquals(product.getOriginalLink(), result.getOriginalLink());
        assertEquals(4, result.getRatings().size());
        assertEquals("Bom", result.getRatings().get(0).getOriginalText());
        assertEquals(SentimentEnum.POSITIVO, result.getRatings().get(0).getSentiment());
        assertEquals(SentimentEnum.NEUTRO, result.getRatings().get(1).getSentiment());
        assertNull(result.getRatings().get(2).getSentiment()); assertNull(result.getRatings().get(3));
        assertSame(product, positive.getProduct()); assertSame(positive, product.getRatings().getFirst());
    }

    @Test
    void nullInputsAndNullRatingsRemainNull() {
        assertNull(mapper.toEntity(null)); assertNull(mapper.toResponseDto(null));
        var product = new Product(); product.setRatings(null);
        assertNull(mapper.toResponseDto(product).getRatings());
    }

    private Rating rating(Product product, String text, String sentiment) {
        var rating = new Rating(); rating.setProduct(product); rating.setOriginalText(text); rating.setSentiment(sentiment);
        return rating;
    }
}
