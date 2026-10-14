package com.example.backend.config

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.PropertiesPropertySource
import java.util.Properties

/**
 * Chooses the database before the context starts:
 * - with `DB_URL` set, uses PostgreSQL (Flyway migrations, schema validation);
 * - without `DB_URL`, falls back to an in-memory H2 so the app starts with no
 *   external database (schema from entities, Flyway disabled).
 */
class DatabaseEnvironmentPostProcessor : EnvironmentPostProcessor {
    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        val properties = Properties()
        val dbUrl = environment.getProperty("DB_URL")

        if (dbUrl.isNullOrBlank()) {
            properties["spring.datasource.url"] = "jdbc:h2:mem:calendar;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
            properties["spring.datasource.username"] = "sa"
            properties["spring.datasource.password"] = ""
            properties["spring.jpa.hibernate.ddl-auto"] = "create-drop"
            properties["spring.flyway.enabled"] = "false"
        } else {
            properties["spring.datasource.url"] = dbUrl
            properties["spring.datasource.username"] = environment.getProperty("DB_USER") ?: "postgres"
            properties["spring.datasource.password"] = environment.getProperty("DB_PASSWORD") ?: ""
            properties["spring.jpa.hibernate.ddl-auto"] = "validate"
        }

        environment.propertySources.addFirst(PropertiesPropertySource("databaseConfiguration", properties))
    }
}
