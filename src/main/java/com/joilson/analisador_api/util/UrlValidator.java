package com.joilson.analisador_api.util;

import java.net.URI;
import java.net.URISyntaxException;

public class UrlValidator {
    
    private UrlValidator() {}

    public static boolean isValidHttpUrl(String url) {
        if (url.isEmpty() || url.isBlank()) {
            return false;
        }

        try {
            URI uri = new URI(url);

            return uri.isAbsolute() &&
                (uri.getScheme().equalsIgnoreCase("http") ||
                uri.getScheme().equalsIgnoreCase("https"));
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
