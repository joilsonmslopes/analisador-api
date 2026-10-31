package com.joilson.analisador_api.service.scraper.strategy;

import com.joilson.analisador_api.dto.scrape.ScrapedProductDto;
import com.microsoft.playwright.BrowserContext;

public interface MarketplaceStrategy {
    boolean supports(String url);
    ScrapedProductDto execute(String url, BrowserContext context);
}
