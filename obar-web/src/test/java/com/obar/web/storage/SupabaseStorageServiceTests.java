package com.obar.web.storage;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupabaseStorageServiceTests {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void uploadsUserPhotoWithServiceKeyAndReturnsPublicUrl() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> apiKey = new AtomicReference<>();
        AtomicReference<String> upsert = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/storage/v1/object/user-photo/users/42.png", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            apiKey.set(exchange.getRequestHeaders().getFirst("apikey"));
            upsert.set(exchange.getRequestHeaders().getFirst("x-upsert"));
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        SupabaseStorageService service = new SupabaseStorageService(
                HttpClient.newHttpClient(), baseUrl, "secret", "user-photo", "vehicle-photo");
        MockMultipartFile image = new MockMultipartFile(
                "photo", "avatar.png", "image/png",
                new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 1});

        String result = service.uploadUserPhoto(42, image);

        assertEquals(baseUrl + "/storage/v1/object/public/user-photo/users/42.png", result);
        assertEquals("Bearer secret", authorization.get());
        assertEquals("secret", apiKey.get());
        assertEquals("true", upsert.get());
    }

    @Test
    void rejectsFileWhoseContentsDoNotMatchContentType() {
        SupabaseStorageService service = new SupabaseStorageService(
                HttpClient.newHttpClient(), "https://example.supabase.co", "secret",
                "user-photo", "vehicle-photo");
        MockMultipartFile image = new MockMultipartFile(
                "photo", "fake.png", "image/png", "not an image".getBytes(StandardCharsets.UTF_8));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.uploadUserPhoto(42, image));

        assertEquals(400, exception.getStatusCode().value());
    }
}
