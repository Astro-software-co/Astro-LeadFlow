package com.astro.leadflow.shared.modules.iam.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class RbacAuthorityTest {

    private fun makeUser(role: Role): User = User(
        id = UUID.randomUUID(),
        name = "Test User",
        email = "test@example.com",
        passwordHash = "hash",
        role = role,
    )

    @Test
    fun `MANAGER user should have ROLE_MANAGER and ROLE_DEALERSHIP`() {
        val user = makeUser(Role.MANAGER)
        val authorities = user.getAuthorities().map { it.authority }
        assertTrue(authorities.contains("ROLE_MANAGER"), "Expected ROLE_MANAGER")
        assertTrue(authorities.contains("ROLE_DEALERSHIP"), "Expected ROLE_DEALERSHIP for MANAGER")
    }

    @Test
    fun `SELLER user should have ROLE_SELLER and ROLE_DEALERSHIP`() {
        val user = makeUser(Role.SELLER)
        val authorities = user.getAuthorities().map { it.authority }
        assertTrue(authorities.contains("ROLE_SELLER"), "Expected ROLE_SELLER")
        assertTrue(authorities.contains("ROLE_DEALERSHIP"), "Expected ROLE_DEALERSHIP for SELLER")
    }

    @Test
    fun `ASTRO_ADMIN user should have ROLE_ASTRO_ADMIN and not ROLE_DEALERSHIP`() {
        val user = makeUser(Role.ASTRO_ADMIN)
        val authorities = user.getAuthorities().map { it.authority }
        assertTrue(authorities.contains("ROLE_ASTRO_ADMIN"), "Expected ROLE_ASTRO_ADMIN")
        assertFalse(authorities.contains("ROLE_DEALERSHIP"), "ASTRO_ADMIN should not have ROLE_DEALERSHIP")
    }

    @Test
    fun `Role enum contains only supported roles`() {
        val roleNames = Role.entries.map { it.name }
        assertEquals(listOf("ASTRO_ADMIN", "MANAGER", "SELLER"), roleNames)
    }
}
