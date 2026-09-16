package com.asms.springasms.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DotenvLoader {

    private DotenvLoader() {
    }

    public static Map<String, Object> loadProperties() {
        File envFile = findEnvFile();
        Map<String, Object> result = new HashMap<>();
        if (envFile == null || !envFile.exists()) {
            System.out.println("[DotenvLoader] No .env file found; falling back to system environment.");
            return result;
        }

        System.out.println("[DotenvLoader] Loading environment properties from: " + envFile.getAbsolutePath());
        Map<String, String> envMap = parseEnvFile(envFile);

        String dbConn = envMap.get("DB_CONN");
        if (dbConn != null && !dbConn.isBlank()) {
            parseDbConnIntoMap(dbConn, result);
        }

        putIfPresent(result, "JWT_SIGNING_KEY", envMap.get("Jwt__SigningKey"));
        putIfPresent(result, "app.jwt.signing-key", envMap.get("Jwt__SigningKey"));

        putIfPresent(result, "JWT_ISSUER", envMap.get("Jwt__Issuer"));
        putIfPresent(result, "app.jwt.issuer", envMap.get("Jwt__Issuer"));

        putIfPresent(result, "JWT_AUDIENCE", envMap.get("Jwt__Audience"));
        putIfPresent(result, "app.jwt.audience", envMap.get("Jwt__Audience"));

        for (Map.Entry<String, String> entry : envMap.entrySet()) {
            putIfPresent(result, entry.getKey(), entry.getValue());
        }

        // Also set system properties for any components reading System.getProperty directly
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            if (System.getProperty(entry.getKey()) == null && System.getenv(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue().toString());
            }
        }

        return result;
    }

    public static void load() {
        loadProperties();
    }

    private static File findEnvFile() {
        List<Path> candidatePaths = List.of(
                Paths.get(".env"),
                Paths.get("..", ".env"),
                Paths.get("..", "..", ".env"),
                Paths.get("..", "..", "Backend", "OnnorokomBackend", ".env"),
                Paths.get("..", "Backend", "OnnorokomBackend", ".env"),
                Paths.get("Backend", "OnnorokomBackend", ".env")
        );

        for (Path candidate : candidatePaths) {
            File file = candidate.toFile();
            if (file.exists() && file.isFile()) {
                return file;
            }
        }
        return null;
    }

    private static Map<String, String> parseEnvFile(File file) {
        Map<String, String> result = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIndex = line.indexOf('=');
                if (eqIndex > 0) {
                    String key = line.substring(0, eqIndex).trim();
                    String value = line.substring(eqIndex + 1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    result.put(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("[DotenvLoader] Warning: Failed to read .env file: " + e.getMessage());
        }
        return result;
    }

    private static void parseDbConnIntoMap(String dbConn, Map<String, Object> result) {
        Map<String, String> parts = new HashMap<>();
        for (String segment : dbConn.split(";")) {
            int eqIndex = segment.indexOf('=');
            if (eqIndex > 0) {
                String key = segment.substring(0, eqIndex).trim();
                String value = segment.substring(eqIndex + 1).trim();
                parts.put(key, value);
            }
        }

        String host = parts.get("Host");
        String port = parts.getOrDefault("Port", "5432");
        String database = parts.get("Database");
        String username = parts.get("Username");
        String password = parts.get("Password");

        if (host != null && database != null) {
            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database + "?sslmode=require";
            result.put("SPRING_DATASOURCE_URL", jdbcUrl);
            result.put("spring.datasource.url", jdbcUrl);
            result.put("DB_HOST", host);
            result.put("DB_PORT", port);
            result.put("DB_NAME", database);
        }

        if (username != null) {
            result.put("DB_USER", username);
            result.put("spring.datasource.username", username);
        }

        if (password != null) {
            result.put("DB_PASSWORD", password);
            result.put("spring.datasource.password", password);
        }
    }

    private static void putIfPresent(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank() && !map.containsKey(key)) {
            map.put(key, value);
        }
    }
}
