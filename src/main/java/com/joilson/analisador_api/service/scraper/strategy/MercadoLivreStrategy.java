package com.joilson.analisador_api.service.scraper.strategy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.joilson.analisador_api.dto.scrape.ScrapedProductDto;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;

@Component 
public class MercadoLivreStrategy implements MarketplaceStrategy {
    public boolean supports(String url) {
        return url != null && url.contains("mercadolivre");
    }

    public ScrapedProductDto execute(String url, BrowserContext context) {
        Page page = null;

        try {
            page = context.newPage();

            page.navigate(url, new Page.NavigateOptions().setTimeout(60000));
            page.waitForSelector("h1.ui-pdp-title", new Page.WaitForSelectorOptions().setTimeout(60000));

            ScrapedProductDto scrapedProductDto = new ScrapedProductDto();

            String cleanResolvedUrl = cleanUrl(page.url());
            scrapedProductDto.setOriginalLink(cleanResolvedUrl);

            String title = page.locator("h1.ui-pdp-title").innerText();
            scrapedProductDto.setName(title);

            String priceFraction = page.locator(".andes-money-amount__fraction").first().innerText().replace(".", "");
            scrapedProductDto.setCurrentPrice(new BigDecimal(priceFraction));

            page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
            page.waitForTimeout(2000);

            List<Locator> ratingsLocators = page.locator("p.ui-review-capability-comments__comment__content").all();
        
            List<String> ratings = new ArrayList<>();

            ratingsLocators.forEach(ratingLocator -> {
                String ratingText = ratingLocator.innerText();
                if (!ratingText.isEmpty() && !ratingText.isBlank()) {
                    ratings.add(ratingText);
                }
            });

            scrapedProductDto.setRatings(ratings);

            return scrapedProductDto;
        } catch (PlaywrightException e) {
            throw new RuntimeException("Erro ao controlar o navegador headless: " + e.getMessage());
        } finally {
            if (page != null) {
                page.close();
            }
        }
    }

    private String cleanUrl(String rawUrl) {
        if (rawUrl == null) return null;

        int hashIndex = rawUrl.indexOf("#");
        if (hashIndex != -1) {
            rawUrl = rawUrl.substring(0, hashIndex);
        }

        int queryIndex = rawUrl.indexOf("?");
        if (queryIndex != -1) {
            rawUrl = rawUrl.substring(0, queryIndex);
        }

        return rawUrl;
    }
}
