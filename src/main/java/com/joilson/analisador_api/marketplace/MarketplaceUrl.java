package com.joilson.analisador_api.marketplace;

import java.net.URI;
import java.util.Locale;

final class MarketplaceUrl {
    private MarketplaceUrl() {}

    static URI parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("originalLink é obrigatório");
        }
        final URI uri;
        try {
            uri = URI.create(value.strip());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("originalLink deve ser uma URL HTTP(S) válida", exception);
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))
                || uri.getHost() == null || uri.getRawUserInfo() != null
                || (uri.getPort() != -1 && uri.getPort() != (scheme.equalsIgnoreCase("https") ? 443 : 80))) {
            throw new IllegalArgumentException("originalLink deve ser uma URL HTTP(S) válida, sem credenciais ou porta alternativa");
        }
        return uri;
    }

    static String host(URI uri) {
        return uri.getHost().toLowerCase(Locale.ROOT);
    }
}
