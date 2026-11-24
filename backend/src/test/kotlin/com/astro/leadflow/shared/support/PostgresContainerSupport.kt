package com.astro.leadflow.shared.support

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.time.Duration

/**
 * Postgres 16 real com as migrations Flyway e `ddl-auto=validate` (regra 10 do plano: FK, índice parcial e
 * advisory lock não existem no H2). Um contêiner por JVM, reaproveitado pelas classes que herdam.
 */
abstract class PostgresContainerSupport {
    companion object {
        // Espera por porta: a leitura de log do docker-java antigo não funciona com o Docker 29.
        private val postgres = PostgreSQLContainer("postgres:16-alpine")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(90)))
            .apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun postgresProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
        }
    }
}
