package com.joilson.analisador_api.service.scraper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.stereotype.Service;

import com.joilson.analisador_api.dto.scrape.ScrapedProductDto;
import com.joilson.analisador_api.service.scraper.factory.ScraperFactory;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class ScraperService {

    private Playwright playwright;
    private BrowserContext context;
    private final ScraperFactory scraperFactory;

    @PostConstruct 
    public void initBrowser() {
        this.playwright = Playwright.create();
        Path userDataDir = Paths.get("playwright-profile");

        BrowserType.LaunchPersistentContextOptions options = new BrowserType.LaunchPersistentContextOptions()
            .setHeadless(false)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setViewportSize(1280, 720)
            .setArgs(List.of("--disable-blink-features=AutomationControlled"));

            this.context = playwright.chromium().launchPersistentContext(userDataDir, options);
            this.context.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
    }

    @PreDestroy 
    public void closeBrowser() {
        if (this.context != null) {
            this.context.close();
        }
        if (this.playwright != null) {
            this.playwright.close();
        }
    }


    public ScrapedProductDto scrape(String url) {
        if (url.isBlank() || url.isEmpty()) {
            throw new IllegalArgumentException("Por favor, insira uma URL válida");
        }
        
        var strategy = scraperFactory.getStrategy(url);

        return strategy.execute(url, context);
    }
}
