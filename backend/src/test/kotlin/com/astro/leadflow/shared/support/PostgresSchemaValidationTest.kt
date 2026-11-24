package com.astro.leadflow.shared.support

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.jdbc.core.JdbcTemplate

/** DoD: as migrations V1…Vn aplicam num Postgres 16 limpo e batem com as entidades (`ddl-auto=validate`). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostgresSchemaValidationTest : PostgresContainerSupport() {

    @Autowired lateinit var jdbc: JdbcTemplate

    @Test
    fun `migrations aplicam e as entidades validam contra o schema`() {
        val version = jdbc.queryForObject("SELECT max(version::int) FROM flyway_schema_history", Int::class.java)!!
        assertTrue(version >= 21, "versão aplicada: $version")
    }
}
