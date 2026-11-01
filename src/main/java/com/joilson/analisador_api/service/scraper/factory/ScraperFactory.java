package com.joilson.analisador_api.service.scraper.factory;

import java.util.List;

import org.springframework.stereotype.Component;

import com.joilson.analisador_api.service.scraper.strategy.MarketplaceStrategy;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class ScraperFactory {
    private final List<MarketplaceStrategy> strategies;

    public MarketplaceStrategy getStrategy(String url) {
        return strategies.stream()
            .filter(strategy -> strategy.supports(url))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Marketplace não suportado. No momento, analisamos apenas links do Mercado Livre. " + url));
    }
}
