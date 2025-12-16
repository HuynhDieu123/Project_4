package com.mypack.service;

import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.security.KeyStore;
import java.time.Duration;

@Stateless
public class GoogleOAuth2Service {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";

    /**
     * Main API: exchange code -> access_token -> userinfo
     */
    public GoogleUserInfo getUserInfoFromCode(String code,
                                              String clientId,
                                              String clientSecret,
                                              String redirectUri) {

        try {
            HttpClient client = buildSecureHttpClientFromJdkCacerts();

            String accessToken = exchangeCodeForAccessToken(client, code, clientId, clientSecret, redirectUri);
            return fetchUserInfo(client, accessToken);

        } catch (Exception e) {
            throw new RuntimeException("Google OAuth2 failed: " + e.getMessage(), e);
        }
    }

    private String exchangeCodeForAccessToken(HttpClient client,
                                              String code,
                                              String clientId,
                                              String clientSecret,
                                              String redirectUri) throws Exception {

        String form = "client_id=" + url(clientId)
                + "&client_secret=" + url(clientSecret)
                + "&code=" + url(code)
                + "&grant_type=authorization_code"
                + "&redirect_uri=" + url(redirectUri);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        String body = res.body() == null ? "" : res.body();

        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            JsonObject json = reader.readObject();

            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                String accessToken = json.getString("access_token", null);
                if (accessToken == null || accessToken.isBlank()) {
                    throw new IllegalArgumentException("Token exchange success but missing access_token");
                }
                return accessToken;
            }

            String err = json.getString("error", "unknown_error");
            String desc = json.getString("error_description", body);
            throw new IllegalArgumentException("Token exchange failed: " + err + " - " + desc);
        }
    }

    private GoogleUserInfo fetchUserInfo(HttpClient client, String accessToken) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(USERINFO_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        String body = res.body() == null ? "" : res.body();

        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IllegalArgumentException("UserInfo failed: HTTP " + res.statusCode() + " - " + body);
        }

        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            JsonObject json = reader.readObject();

            GoogleUserInfo info = new GoogleUserInfo();
            info.email = json.getString("email", null);
            info.name = json.getString("name", null);
            info.picture = json.getString("picture", null);
            info.sub = json.getString("sub", null);
            return info;
        }
    }

    /**
     * ✅ Fix PKIX: Always build SSLContext from JDK cacerts (not from GlassFish overridden trustStore)
     */
    private HttpClient buildSecureHttpClientFromJdkCacerts() throws Exception {
        SSLContext sslContext = buildSslContextFromJdkCacerts();
        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private SSLContext buildSslContextFromJdkCacerts() throws Exception {
        Path cacerts = findJdkCacertsPath();

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        try (var in = Files.newInputStream(cacerts)) {
            ks.load(in, "changeit".toCharArray());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, tmf.getTrustManagers(), null);
        return ctx;
    }

    private Path findJdkCacertsPath() {
        String javaHome = System.getProperty("java.home");

        // common: <java.home>/lib/security/cacerts
        Path p1 = Paths.get(javaHome, "lib", "security", "cacerts");
        if (Files.exists(p1)) return p1;

        // some distros: <java.home>/conf/security/cacerts
        Path p2 = Paths.get(javaHome, "conf", "security", "cacerts");
        if (Files.exists(p2)) return p2;

        throw new IllegalStateException("Cannot find cacerts under java.home=" + javaHome);
    }

    private static String url(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    // simple DTO
    public static class GoogleUserInfo {
        public String email;
        public String name;
        public String picture;
        public String sub;
    }
}
