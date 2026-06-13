package com.obar.web.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SupabaseStorageService {

    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final HttpClient httpClient;
    private final String supabaseUrl;
    private final String secretKey;
    private final String userPhotoBucket;
    private final String vehiclePhotoBucket;

    @Autowired
    public SupabaseStorageService(
            @Value("${supabase.url:}") String supabaseUrl,
            @Value("${supabase.secret-key:}") String secretKey,
            @Value("${supabase.user-photo-bucket:user-photos}") String userPhotoBucket,
            @Value("${supabase.vehicle-photo-bucket:vehicle-photos}") String vehiclePhotoBucket) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                supabaseUrl, secretKey, userPhotoBucket, vehiclePhotoBucket);
    }

    SupabaseStorageService(HttpClient httpClient, String supabaseUrl, String secretKey,
            String userPhotoBucket, String vehiclePhotoBucket) {
        this.httpClient = httpClient;
        this.supabaseUrl = stripTrailingSlash(supabaseUrl);
        this.secretKey = secretKey == null ? "" : secretKey.trim();
        this.userPhotoBucket = userPhotoBucket;
        this.vehiclePhotoBucket = vehiclePhotoBucket;
    }

    public String uploadUserPhoto(Integer userId, MultipartFile file) {
        return upload(userPhotoBucket, "users/" + userId, file);
    }

    public String uploadVehiclePhoto(Integer vehicleId, MultipartFile file) {
        return upload(vehiclePhotoBucket, "vehicles/" + vehicleId, file);
    }

    private String upload(String bucket, String objectPrefix, MultipartFile file) {
        String contentType = validate(file);
        requireConfiguration();

        String objectPath = objectPrefix + "." + EXTENSIONS.get(contentType);
        URI uploadUri = URI.create(supabaseUrl + "/storage/v1/object/"
                + encodePathSegment(bucket) + "/" + encodePath(objectPath));

        try {
            HttpRequest request = HttpRequest.newBuilder(uploadUri)
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + secretKey)
                    .header("apikey", secretKey)
                    .header("Content-Type", contentType)
                    .header("x-upsert", "true")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        storageErrorMessage(response.statusCode(), response.body()));
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Nao foi possivel ler ou enviar a imagem.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "O upload da imagem foi interrompido.", exception);
        }

        return supabaseUrl + "/storage/v1/object/public/"
                + encodePathSegment(bucket) + "/" + encodePath(objectPath);
    }

    private String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolhe uma imagem para enviar.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A imagem nao pode exceder 5 MB.");
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Formato invalido. Usa JPEG, PNG ou WebP.");
        }
        try {
            if (!matchesFileSignature(contentType, file.getBytes())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O conteudo do ficheiro nao corresponde a uma imagem valida.");
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nao foi possivel ler a imagem.", exception);
        }
        return contentType;
    }

    private boolean matchesFileSignature(String contentType, byte[] bytes) {
        return switch (contentType) {
            case "image/jpeg" -> bytes.length >= 3
                    && unsigned(bytes[0]) == 0xff
                    && unsigned(bytes[1]) == 0xd8
                    && unsigned(bytes[2]) == 0xff;
            case "image/png" -> bytes.length >= 8
                    && unsigned(bytes[0]) == 0x89
                    && bytes[1] == 'P'
                    && bytes[2] == 'N'
                    && bytes[3] == 'G'
                    && unsigned(bytes[4]) == 0x0d
                    && unsigned(bytes[5]) == 0x0a
                    && unsigned(bytes[6]) == 0x1a
                    && unsigned(bytes[7]) == 0x0a;
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 'R'
                    && bytes[1] == 'I'
                    && bytes[2] == 'F'
                    && bytes[3] == 'F'
                    && bytes[8] == 'W'
                    && bytes[9] == 'E'
                    && bytes[10] == 'B'
                    && bytes[11] == 'P';
            default -> false;
        };
    }

    private int unsigned(byte value) {
        return Byte.toUnsignedInt(value);
    }

    private void requireConfiguration() {
        if (supabaseUrl.isBlank() || secretKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "O armazenamento de imagens ainda nao esta configurado.");
        }
    }

    private String storageErrorMessage(int statusCode, String responseBody) {
        if (statusCode == 404) {
            return "O bucket de imagens nao foi encontrado no Supabase.";
        }
        if (statusCode == 401 || statusCode == 403) {
            return "A chave do Supabase nao tem permissao para enviar imagens.";
        }
        if (responseBody != null && responseBody.toLowerCase(Locale.ROOT).contains("bucket")) {
            return "O Supabase recusou o bucket configurado para esta imagem.";
        }
        return "O Supabase recusou o upload da imagem (HTTP " + statusCode + ").";
    }

    private static String encodePath(String path) {
        return String.join("/", path.split("/"))
                .replace(" ", "%20");
    }

    private static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String stripTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("/+$", "");
    }
}
