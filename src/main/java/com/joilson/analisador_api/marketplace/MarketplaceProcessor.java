package com.joilson.analisador_api.marketplace;

import com.joilson.analisador_api.dto.scrape.ScrapedProductDto;

public interface MarketplaceProcessor {
    String marketplaceId();

    boolean supportsHost(String host);

    ScrapedProductDto process(ScrapedProductDto input);
}
