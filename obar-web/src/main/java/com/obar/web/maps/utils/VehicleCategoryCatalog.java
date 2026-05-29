package com.obar.web.maps.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

public final class VehicleCategoryCatalog {

    private static final List<String> SUPPORTED_CATEGORIES = List.of("STANDARD", "XL", "PREMIUM");
    private static final Set<String> SUPPORTED_CATEGORY_SET = Set.copyOf(SUPPORTED_CATEGORIES);

    private VehicleCategoryCatalog() {
    }

    public static List<String> supported() {
        return SUPPORTED_CATEGORIES;
    }

    public static String normalize(String category) {
        if (category == null || category.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolhe o tipo de veículo.");
        }

        String normalized = category.trim().toUpperCase();
        if (!SUPPORTED_CATEGORY_SET.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de veículo inválido.");
        }
        return normalized;
    }
}
