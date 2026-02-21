package com.astro.leadflow.shared.modules.ingestion.service

import com.astro.leadflow.modules.ingestion.service.NormalizationService
import com.astro.leadflow.shared.exception.BusinessException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NormalizationServiceTest {

    private val service = NormalizationService()

    @Test
    fun `normalizePhone should handle format with parentheses and hyphen`() {
        Assertions.assertEquals("+5511987654321", service.normalizePhone("(11) 98765-4321"))
    }

    @Test
    fun `normalizePhone should handle 11 digits without formatting`() {
        Assertions.assertEquals("+5511987654321", service.normalizePhone("11987654321"))
    }

    @Test
    fun `normalizePhone should handle number with country code`() {
        Assertions.assertEquals("+5511987654321", service.normalizePhone("5511987654321"))
    }

    @Test
    fun `normalizePhone should handle number with plus sign`() {
        Assertions.assertEquals("+5511987654321", service.normalizePhone("+5511987654321"))
    }

    @Test
    fun `normalizePhone should throw for invalid number`() {
        assertThrows<BusinessException> {
            service.normalizePhone("123")
        }
    }

    @Test
    fun `normalizeMarketplace should uppercase and trim`() {
        Assertions.assertEquals("WEBMOTORS", service.normalizeMarketplace("  webmotors  "))
        Assertions.assertEquals("MERCADO_LIVRE", service.normalizeMarketplace("mercado livre"))
        Assertions.assertEquals("OLX", service.normalizeMarketplace("OLX"))
    }
}