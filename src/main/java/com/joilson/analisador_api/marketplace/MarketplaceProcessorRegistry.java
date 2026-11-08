package com.joilson.analisador_api.marketplace;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.joilson.analisador_api.dto.scrape.ScrapedProductDto;

@Component
public class MarketplaceProcessorRegistry {
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]*");
    private final Map<String, MarketplaceProcessor> processors;

    public MarketplaceProcessorRegistry(List<MarketplaceProcessor> implementations) {
        Map<String, MarketplaceProcessor> registered = new LinkedHashMap<>();
        
        for (var processor : implementations) {
            String id = processor.marketplaceId();

            if (id == null || !ID.matcher(id).matches()) {
                throw new IllegalStateException("Identificador de marketplace inválido: " + id);
            }

            if (registered.putIfAbsent(id, processor) != null) {
                throw new IllegalStateException("Marketplace duplicado: " + id);
            }
        }
        processors = Map.copyOf(registered);
    }

    public ScrapedProductDto process(ScrapedProductDto input) {
        if (input == null) {
            throw new IllegalArgumentException("Dados do produto são obrigatórios");
        }

        String host = MarketplaceUrl.host(MarketplaceUrl.parse(input.getOriginalLink()));
        MarketplaceProcessor processor;

        if (input.getMarketplace() != null) {
            processor = processors.get(input.getMarketplace());

            if (processor == null) {
                throw new IllegalArgumentException("Marketplace não suportado: " + input.getMarketplace());
            }
        } else {
            var matches = processors.values().stream().filter(candidate -> candidate.supportsHost(host)).toList();
            if (matches.isEmpty()) {
                throw new IllegalArgumentException("Marketplace não suportado para domínio: " + host);
            }
            if (matches.size() != 1) {
                throw new IllegalArgumentException("Domínio ambíguo; informe marketplace explicitamente: " + host);
            }
            processor = matches.getFirst();
        }

        if (!processor.supportsHost(host)) {
            throw new IllegalArgumentException("Marketplace não corresponde ao domínio da URL: " + host);
        }

        return processor.process(input);
    }
}
