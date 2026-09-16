package com.asms.springasms;

import static org.junit.jupiter.api.Assertions.*;

import com.asms.springasms.config.DotenvLoader;
import org.junit.jupiter.api.Test;

class DotenvLoaderTest {

    @Test
    void load_shouldFindAndPopulateProperties() {
        DotenvLoader.load();

        String dbUrl = System.getProperty("spring.datasource.url");
        if (dbUrl == null) {
            dbUrl = System.getProperty("SPRING_DATASOURCE_URL");
        }

        assertNotNull(dbUrl, "Datasource URL should be populated by DotenvLoader");
        assertTrue(dbUrl.startsWith("jdbc:postgresql://"), "Datasource URL should be a postgres JDBC URL");

        String jwtKey = System.getProperty("JWT_SIGNING_KEY");
        if (jwtKey == null) {
            jwtKey = System.getProperty("app.jwt.signing-key");
        }
        assertNotNull(jwtKey, "JWT signing key should be populated by DotenvLoader");
    }
}
