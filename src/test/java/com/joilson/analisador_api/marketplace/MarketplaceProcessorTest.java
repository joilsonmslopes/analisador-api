package com.joilson.analisador_api.marketplace;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.joilson.analisador_api.dto.scrape.ScrapedProductDto;

class MarketplaceProcessorTest {
    private final MercadoLivreProcessor processor = new MercadoLivreProcessor();
    private final MarketplaceProcessorRegistry registry = new MarketplaceProcessorRegistry(List.of(processor));

    @Test
    void normalizesCopyAndInfersLegacyMarketplace() {
        var input = input(" HTTPS://PRODUTO.MERCADOLIVRE.COM.BR:443/MLB-123?a=%2F#reviews ");
        input.setName("  Mouse\n gamer  ");
        input.setRatings(Arrays.asList(null, "  ", " Muito\n bom ", "Muito bom"));
        var result = registry.process(input);
        assertEquals("mercado_livre", result.getMarketplace());
        assertEquals("Mouse gamer", result.getName());
        assertEquals("https://produto.mercadolivre.com.br/MLB-123?a=%2F", result.getOriginalLink());
        assertEquals(List.of("Muito bom", "Muito bom"), result.getRatings());
        assertEquals(new BigDecimal("19.90"), result.getCurrentPrice());
        assertNull(input.getMarketplace());
        assertNotSame(input, result);
    }

    @Test
    void acceptsExplicitStableIdAndMissingRatings() {
        var input = input("https://mercadolivre.com.br/p/MLB123");
        input.setMarketplace(MercadoLivreProcessor.ID);
        assertTrue(registry.process(input).getRatings().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "amazon", "MERCADO_LIVRE", " mercado_livre "})
    void rejectsUnknownExplicitIdWithoutFallingBack(String id) {
        var input = input("https://mercadolivre.com.br/p/MLB123");
        input.setMarketplace(id);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> registry.process(input))
                .getMessage().contains("não suportado"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com/p", "https://mercadolivre.com.br.evil.com/p",
            "https://evilmercadolivre.com.br/p"})
    void rejectsUnknownDomainsAndExplicitMismatch(String url) {
        var input = input(url);
        assertThrows(IllegalArgumentException.class, () -> registry.process(input));
        input.setMarketplace(MercadoLivreProcessor.ID);
        assertThrows(IllegalArgumentException.class, () -> registry.process(input));
        assertThrows(IllegalArgumentException.class, () -> processor.process(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "not a url", "/relative", "//mercadolivre.com.br/p", "ftp://mercadolivre.com.br/p",
            "https://mercadolivre.com.br:8080/p", "https://user@mercadolivre.com.br/p",
            "https://mercadolivre.com.br\\@evil.com", "https://mercadolivre.com.br/%ZZ"})
    void rejectsInvalidUrls(String url) {
        assertThrows(IllegalArgumentException.class, () -> registry.process(input(url)));
    }

    @Test
    void rejectsInvalidProductFieldsAndNullInput() {
        assertThrows(IllegalArgumentException.class, () -> registry.process(null));
        var input = input("https://mercadolivre.com.br/p");
        input.setName(" ");
        assertThrows(IllegalArgumentException.class, () -> registry.process(input));
        input.setName("Mouse");
        input.setCurrentPrice(null);
        assertThrows(IllegalArgumentException.class, () -> registry.process(input));
        input.setCurrentPrice(new BigDecimal("-1"));
        assertThrows(IllegalArgumentException.class, () -> registry.process(input));
    }

    @Test
    void failsFastOnDuplicateRegistrationsAndInvalidIds() {
        assertTrue(assertThrows(IllegalStateException.class,
                () -> new MarketplaceProcessorRegistry(List.of(processor, new MercadoLivreProcessor())))
                .getMessage().contains("duplicado"));
        var invalid = mock(MarketplaceProcessor.class);
        when(invalid.marketplaceId()).thenReturn(" ");
        assertThrows(IllegalStateException.class, () -> new MarketplaceProcessorRegistry(List.of(invalid)));
    }

    @Test
    void delegatesToInjectedStrategyAndRejectsAmbiguousInference() {
        var additional = mock(MarketplaceProcessor.class);
        when(additional.marketplaceId()).thenReturn("test_marketplace");
        when(additional.supportsHost("mercadolivre.com.br")).thenReturn(true);
        var extensible = new MarketplaceProcessorRegistry(List.of(processor, additional));
        var input = input("https://mercadolivre.com.br/p");
        assertThrows(IllegalArgumentException.class, () -> extensible.process(input));
        input.setMarketplace("test_marketplace");
        var normalized = new ScrapedProductDto();
        when(additional.process(input)).thenReturn(normalized);
        assertSame(normalized, extensible.process(input));
        verify(additional).process(input);
        assertThrows(IllegalArgumentException.class,
                () -> new MarketplaceProcessorRegistry(List.of()).process(input));
    }

    private ScrapedProductDto input(String url) {
        var input = new ScrapedProductDto();
        input.setName("Mouse");
        input.setCurrentPrice(new BigDecimal("19.90"));
        input.setOriginalLink(url);
        return input;
    }
}
