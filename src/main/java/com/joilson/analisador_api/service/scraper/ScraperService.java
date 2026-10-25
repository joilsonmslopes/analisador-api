package com.joilson.analisador_api.service.scraper;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.stereotype.Service;

import com.joilson.analisador_api.model.Product;
import com.joilson.analisador_api.model.Rating;
import com.joilson.analisador_api.service.AiService;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class ScraperService {

    private final AiService aiService;
    
    public Product scrapeMercadoLivre(String url) {
        try (Playwright playwright = Playwright.create()) {
            Path userDataDir = Paths.get("playwright-profile");

            BrowserType.LaunchPersistentContextOptions options = new BrowserType.LaunchPersistentContextOptions()
                .setHeadless(false)
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setViewportSize(1280, 720)
                .setArgs(List.of("--disable-blink-features=AutomationControlled"));

            BrowserContext context = playwright.chromium().launchPersistentContext(userDataDir, options);

            context.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined)");

            Page page = context.pages().get(0);

            page.navigate(url, new Page.NavigateOptions().setTimeout(60000));

            page.waitForSelector("h1.ui-pdp-title", new Page.WaitForSelectorOptions().setTimeout(60000));

            Product product = new Product();
            product.setOriginalLink(url);

            String title = page.locator("h1.ui-pdp-title").innerText();
            product.setName(title);

            String priceFraction = page.locator(".andes-money-amount__fraction").first().innerText().replace(".", "");
            product.setCurrentPrice(new BigDecimal(priceFraction));

            page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
            page.waitForTimeout(2000);

            List<Locator> ratingsLocators = page.locator("p.ui-review-capability-comments__comment__content").all();
        
            for (Locator ratingLocator : ratingsLocators) {
                String ratingText = ratingLocator.innerText();

                if (!ratingText.isEmpty() && !ratingText.isBlank()) {
                    Rating rating = new Rating();
                    rating.setOriginalText(ratingText);

                    String sentiment = aiService.analyzeSentiment(ratingText);
                    rating.setSentiment(sentiment);

                    rating.setProduct(product);

                    product.getRatings().add(rating);
                }

            }

            return product;
        } catch (PlaywrightException e) {
            throw new RuntimeException("Erro ao controlar o navegador headless: " + e.getMessage());
        }
    }
}
