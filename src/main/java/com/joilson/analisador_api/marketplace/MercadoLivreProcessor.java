package com.joilson.analisador_api.marketplace;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.joilson.analisador_api.dto.scrape.ScrapedProductDto;

@Component
public class MercadoLivreProcessor implements MarketplaceProcessor {
    public static final String ID = "mercado_livre";
    private static final String DOMAIN = "mercadolivre.com.br";

    @Override
    public String marketplaceId() {
        return ID;
    }

    @Override
    public boolean supportsHost(String host) {
        if (host == null) {
            return false;
        }

        String normalized = host.toLowerCase(Locale.ROOT);
        return normalized.equals(DOMAIN) || normalized.endsWith("." + DOMAIN);
    }

    @Override
    public ScrapedProductDto process(ScrapedProductDto input) {
        if (input == null) {
            throw new IllegalArgumentException("Dados do produto são obrigatórios");  
        }

        var uri = MarketplaceUrl.parse(input.getOriginalLink());

        if (!supportsHost(MarketplaceUrl.host(uri))
                || (input.getMarketplace() != null && !ID.equals(input.getMarketplace()))) {
            throw new IllegalArgumentException("Marketplace não corresponde ao domínio da URL");
        }

        if (input.getName() == null || input.getName().isBlank()) {
            throw new IllegalArgumentException("Nome do produto é obrigatório");
        }

        if (input.getCurrentPrice() == null || input.getCurrentPrice().signum() < 0) {
            throw new IllegalArgumentException("Preço do produto deve ser informado e não negativo");
        }

        var scrapedProductDto = new ScrapedProductDto();
        scrapedProductDto.setMarketplace(ID);
        scrapedProductDto.setName(cleanText(input.getName()));
        scrapedProductDto.setCurrentPrice(input.getCurrentPrice());
        // Preserve raw path escaping and query parameters, which may identify a variation.
        scrapedProductDto.setOriginalLink(uri.getScheme().toLowerCase(Locale.ROOT) + "://"
                + MarketplaceUrl.host(uri) + (uri.getRawPath().isEmpty() ? "/" : uri.getRawPath())
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery()));

        scrapedProductDto.setRatings(input.getRatings() == null ? List.of() : input.getRatings().stream()
                .filter(Objects::nonNull).map(MercadoLivreProcessor::cleanText)
                .filter(text -> !text.isBlank()).toList());
                
        return scrapedProductDto;
    }

    private static String cleanText(String value) {
        return value.replaceAll("(?U)\\s+", " ").strip();
    }
}
