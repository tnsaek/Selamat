package com.tinsae.socialmediaplatform.integration;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("it")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractIntegrationTest {

    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern REFRESH_TOKEN_PATTERN = Pattern.compile("\"refreshToken\"\\s*:\\s*\"([^\"]+)\"");

    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0")
            .withDatabaseName("selamat_it")
            .withUsername("selamat")
            .withPassword("selamat");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        MYSQL.start();
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    protected HttpResponse<String> signUp(String username, String email) throws Exception {
        return signUp(username, email, "StrongPassword123");
    }

    protected HttpResponse<String> signUp(String username, String email, String password) throws Exception {
        return postJson("/api/auth/signup", """
                {
                  "username": "%s",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(username, email, password));
    }

    protected HttpResponse<String> login(String identifier, String password) throws Exception {
        return postJson("/api/auth/login", """
                {
                  "identifier": "%s",
                  "password": "%s"
                }
                """.formatted(identifier, password));
    }

    protected HttpResponse<String> get(String path, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> postJson(String path, String body) throws Exception {
        return postJson(path, body, null);
    }

    protected HttpResponse<String> postJson(String path, String body, String accessToken) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (accessToken != null) {
            requestBuilder.header("Authorization", "Bearer " + accessToken);
        }

        return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> putJson(String path, String body, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> postMultipart(
            String path,
            String fileFieldName,
            String filename,
            String contentType,
            byte[] fileContent,
            Map<String, String> fields,
            String accessToken
    ) throws Exception {
        String boundary = "----SelamatIntegrationTest" + UUID.randomUUID();
        List<byte[]> bodyParts = new ArrayList<>();

        for (Map.Entry<String, String> field : fields.entrySet()) {
            bodyParts.add(("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + field.getKey() + "\"\r\n\r\n"
                    + field.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
        }

        bodyParts.add(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + fileFieldName + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        bodyParts.add(fileContent);
        bodyParts.add(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofByteArrays(bodyParts))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> patchJson(String path, String body, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> delete(String path, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + accessToken)
                .DELETE()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    protected String extractAccessToken(String body) {
        return extract(body, ACCESS_TOKEN_PATTERN);
    }

    protected String extractRefreshToken(String body) {
        return extract(body, REFRESH_TOKEN_PATTERN);
    }

    protected String extractStringField(String body, String fieldName) {
        return extract(body, Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]+)\""));
    }

    private String extract(String body, Pattern pattern) {
        Matcher matcher = pattern.matcher(body);
        assertThat(matcher.find()).as("Response body should contain token field").isTrue();
        return matcher.group(1);
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
