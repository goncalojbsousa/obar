package com.obar.web.maps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class MapsServiceClient {

    private static final BigDecimal BASE_FARE = new BigDecimal("3.50");
    private static final BigDecimal PRICE_PER_KM = new BigDecimal("0.85");
    private static final BigDecimal PRICE_PER_MINUTE = new BigDecimal("0.08");
    private static final BigDecimal MINIMUM_FARE = new BigDecimal("4.50");

    private final String openRouteServiceApiKey;
    private final String openRouteServiceBaseUrl;
    private final String nominatimBaseUrl;
    private final String osrmBaseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MapsServiceClient(
            @Value("${openrouteservice.api-key:}") String apiKey,
            @Value("${openrouteservice.base-url:https://api.openrouteservice.org}") String openRouteServiceBaseUrl,
            @Value("${nominatim.base-url:https://nominatim.openstreetmap.org}") String nominatimBaseUrl,
            @Value("${osrm.base-url:https://router.project-osrm.org}") String osrmBaseUrl) {
        this.openRouteServiceApiKey = apiKey == null ? "" : apiKey.trim();
        this.openRouteServiceBaseUrl = trimTrailingSlash(openRouteServiceBaseUrl);
        this.nominatimBaseUrl = trimTrailingSlash(nominatimBaseUrl);
        this.osrmBaseUrl = trimTrailingSlash(osrmBaseUrl);
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    public List<LocationSuggestion> autocomplete(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        if (openRouteServiceApiKey.isBlank()) {
            return searchWithNominatim(text);
        }

        try {
            return searchWithOpenRouteService(text);
        } catch (ResponseStatusException ex) {
            return searchWithNominatim(text);
        }
    }

    private List<LocationSuggestion> searchWithOpenRouteService(String text) {
        URI uri = URI.create(openRouteServiceBaseUrl + "/geocode/autocomplete?api_key=" + encode(openRouteServiceApiKey)
                + "&text=" + encode(text.trim())
                + "&size=6");
        JsonNode json = sendMapRequest(HttpRequest.newBuilder(uri).GET().build(), "openrouteservice");

        List<LocationSuggestion> suggestions = new ArrayList<>();
        for (JsonNode feature : json.path("features")) {
            JsonNode coordinates = feature.path("geometry").path("coordinates");
            if (!coordinates.isArray() || coordinates.size() < 2) {
                continue;
            }

            JsonNode properties = feature.path("properties");
            String label = properties.path("label").asText(properties.path("name").asText(""));
            if (label.isBlank()) {
                continue;
            }

            suggestions.add(new LocationSuggestion(
                    label,
                    coordinates.get(1).asDouble(),
                    coordinates.get(0).asDouble()));
        }
        return suggestions;
    }

    private List<LocationSuggestion> searchWithNominatim(String text) {
        URI uri = URI.create(nominatimBaseUrl + "/search?q=" + encode(text.trim())
                + "&format=jsonv2"
                + "&addressdetails=1"
                + "&limit=6"
                + "&countrycodes=pt"
                + "&accept-language=pt");
        JsonNode json = sendMapRequest(HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .header("User-Agent", "Obar/1.0")
                .GET()
                .build(), "Nominatim");

        List<LocationSuggestion> suggestions = new ArrayList<>();
        if (!json.isArray()) {
            return suggestions;
        }

        for (JsonNode place : json) {
            String label = place.path("display_name").asText("");
            if (label.isBlank()) {
                continue;
            }

            suggestions.add(new LocationSuggestion(
                    label,
                    place.path("lat").asDouble(),
                    place.path("lon").asDouble()));
        }
        return suggestions;
    }

    public LocationSuggestion reverseGeocode(double lat, double lng) {
        if (!isLatitude(lat) || !isLongitude(lng)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordenadas inválidas.");
        }

        URI uri = URI.create(nominatimBaseUrl + "/reverse?lat=" + lat
                + "&lon=" + lng
                + "&format=jsonv2"
                + "&addressdetails=1"
                + "&accept-language=pt");
        JsonNode json = sendMapRequest(HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .header("User-Agent", "Obar/1.0")
                .GET()
                .build(), "Nominatim");

        String label = json.path("display_name").asText("");
        if (label.isBlank()) {
            label = "Localização atual";
        }

        return new LocationSuggestion(label, lat, lng);
    }

    public RouteEstimate estimate(RouteEstimateRequest request) {
        validateCoordinates(request);

        if (openRouteServiceApiKey.isBlank()) {
            return estimateWithOsrm(request);
        }

        try {
            return estimateWithOpenRouteService(request);
        } catch (ResponseStatusException ex) {
            return estimateWithOsrm(request);
        }
    }

    private RouteEstimate estimateWithOpenRouteService(RouteEstimateRequest request) {
        String body;
        try {
            body = objectMapper.writeValueAsString(new DirectionsRequest(
                    List.of(
                            List.of(request.originLng(), request.originLat()),
                            List.of(request.destinationLng(), request.destinationLat()))));
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível preparar o pedido de rota.",
                    ex);
        }

        HttpRequest httpRequest = HttpRequest.newBuilder(
                URI.create(openRouteServiceBaseUrl + "/v2/directions/driving-car/geojson"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", openRouteServiceApiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/geo+json, application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        JsonNode routeJson = sendMapRequest(httpRequest, "openrouteservice");
        JsonNode feature = routeJson.path("features").isArray() && !routeJson.path("features").isEmpty()
                ? routeJson.path("features").get(0)
                : null;
        if (feature == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "A openrouteservice não devolveu uma rota.");
        }

        JsonNode summary = feature.path("properties").path("summary");
        double distanceKm = summary.path("distance").asDouble(0) / 1000.0;
        int durationMin = Math.max(1, (int) Math.ceil(summary.path("duration").asDouble(0) / 60.0));
        BigDecimal estimatedPrice = estimatePrice(distanceKm, durationMin);

        return new RouteEstimate(
                round(distanceKm, 2),
                durationMin,
                estimatedPrice,
                toPlainJson(feature.path("geometry")));
    }

    private RouteEstimate estimateWithOsrm(RouteEstimateRequest request) {
        URI uri = URI.create(osrmBaseUrl + "/route/v1/driving/"
                + request.originLng() + "," + request.originLat()
                + ";"
                + request.destinationLng() + "," + request.destinationLat()
                + "?overview=full&geometries=geojson&steps=false");
        JsonNode routeJson = sendMapRequest(HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .GET()
                .build(), "OSRM");
        JsonNode route = routeJson.path("routes").isArray() && !routeJson.path("routes").isEmpty()
                ? routeJson.path("routes").get(0)
                : null;
        if (route == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi encontrada uma rota por estrada.");
        }

        double distanceKm = route.path("distance").asDouble(0) / 1000.0;
        int durationMin = Math.max(1, (int) Math.ceil(route.path("duration").asDouble(0) / 60.0));
        BigDecimal estimatedPrice = estimatePrice(distanceKm, durationMin);

        return new RouteEstimate(
                round(distanceKm, 2),
                durationMin,
                estimatedPrice,
                toPlainJson(route.path("geometry")));
    }

    private JsonNode sendMapRequest(HttpRequest request, String serviceName) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Erro no serviço de mapas " + serviceName + " (" + response.statusCode() + ").");
            }
            return objectMapper.readTree(response.body());
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Não foi possível contactar o serviço de mapas " + serviceName + ".",
                    ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Pedido ao serviço de mapas " + serviceName + " interrompido.",
                    ex);
        }
    }

    private void validateCoordinates(RouteEstimateRequest request) {
        if (request == null
                || !isLatitude(request.originLat())
                || !isLongitude(request.originLng())
                || !isLatitude(request.destinationLat())
                || !isLongitude(request.destinationLng())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordenadas inválidas.");
        }
    }

    private boolean isLatitude(double value) {
        return value >= -90 && value <= 90;
    }

    private boolean isLongitude(double value) {
        return value >= -180 && value <= 180;
    }

    private BigDecimal estimatePrice(double distanceKm, int durationMin) {
        BigDecimal price = BASE_FARE
                .add(BigDecimal.valueOf(distanceKm).multiply(PRICE_PER_KM))
                .add(BigDecimal.valueOf(durationMin).multiply(PRICE_PER_MINUTE));
        return price.max(MINIMUM_FARE).setScale(2, RoundingMode.HALF_UP);
    }

    private Object toPlainJson(JsonNode node) {
        try {
            return objectMapper.readValue(node.toString(), Object.class);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Formato de geometria inválido.", ex);
        }
    }

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String trimTrailingSlash(String value) {
        String trimmed = value == null || value.isBlank() ? "" : value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private record DirectionsRequest(List<List<Double>> coordinates) {
    }
}
