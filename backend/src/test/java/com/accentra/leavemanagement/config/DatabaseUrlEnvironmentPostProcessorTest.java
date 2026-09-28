package com.accentra.leavemanagement.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();

    @Test
    void mapsRenderStyleUrlToJdbcProperties() {
        Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.toDatasourceProperties(
                "postgres://leave_user:s%40cret@dpg-abc.oregon-postgres.render.com:5432/leave_db");

        assertThat(props).containsEntry("spring.datasource.url",
                        "jdbc:postgresql://dpg-abc.oregon-postgres.render.com:5432/leave_db")
                .containsEntry("spring.datasource.username", "leave_user")
                .containsEntry("spring.datasource.password", "s@cret");
    }

    @Test
    void keepsQueryParametersAndDefaultPort() {
        Map<String, Object> props = DatabaseUrlEnvironmentPostProcessor.toDatasourceProperties(
                "postgresql://u:p@ep-cool.neon.tech/neondb?sslmode=require");

        assertThat(props).containsEntry("spring.datasource.url", "jdbc:postgresql://ep-cool.neon.tech/neondb?sslmode=require");
    }

    @Test
    void rejectsNonPostgresUrls() {
        assertThatThrownBy(() -> DatabaseUrlEnvironmentPostProcessor.toDatasourceProperties("mysql://u:p@h/db"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void explicitSpringDatasourceUrlWins() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("DATABASE_URL", "postgres://u:p@h:5432/db")
                .withProperty("SPRING_DATASOURCE_URL", "jdbc:postgresql://other/db");
        processor.postProcessEnvironment(env, null);
        assertThat(env.getPropertySources().contains(DatabaseUrlEnvironmentPostProcessor.SOURCE_NAME)).isFalse();
    }

    @Test
    void appliesWhenDatabaseUrlIsSet() {
        MockEnvironment env = new MockEnvironment().withProperty("DATABASE_URL", "postgres://u:p@h:5432/db");
        env.getPropertySources().addLast(new MapPropertySource("yml", Map.of("spring.datasource.url", "jdbc:postgresql://localhost/x")));
        processor.postProcessEnvironment(env, null);
        assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://h:5432/db");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("u");
    }
}
