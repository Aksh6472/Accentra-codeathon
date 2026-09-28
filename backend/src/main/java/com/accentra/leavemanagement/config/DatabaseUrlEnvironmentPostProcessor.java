package com.accentra.leavemanagement.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Accepts a platform-style {@code DATABASE_URL} ({@code postgres://user:pass@host:port/db?sslmode=require}),
 * as provided by Render, Neon and similar hosts, and maps it to the JDBC datasource properties.
 * <p>
 * It only applies when {@code DATABASE_URL} is set and {@code SPRING_DATASOURCE_URL} is not, so the
 * {@code DB_*} variables used in local development keep working unchanged.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String SOURCE_NAME = "databaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank() || environment.containsProperty("SPRING_DATASOURCE_URL")) {
            return;
        }
        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, toDatasourceProperties(databaseUrl)));
    }

    static Map<String, Object> toDatasourceProperties(String databaseUrl) {
        URI uri = URI.create(databaseUrl.trim());
        String scheme = uri.getScheme();
        if (!"postgres".equals(scheme) && !"postgresql".equals(scheme)) {
            throw new IllegalStateException("DATABASE_URL must start with postgres:// or postgresql://");
        }
        if (uri.getHost() == null || uri.getPath() == null || uri.getPath().length() <= 1) {
            throw new IllegalStateException("DATABASE_URL must include a host and a database name");
        }
        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
        if (uri.getPort() != -1) {
            jdbc.append(':').append(uri.getPort());
        }
        jdbc.append(uri.getRawPath());
        if (uri.getRawQuery() != null) {
            jdbc.append('?').append(uri.getRawQuery());
        }

        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", jdbc.toString());
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            int colon = userInfo.indexOf(':');
            props.put("spring.datasource.username", decode(colon >= 0 ? userInfo.substring(0, colon) : userInfo));
            if (colon >= 0) {
                props.put("spring.datasource.password", decode(userInfo.substring(colon + 1)));
            }
        }
        return props;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
