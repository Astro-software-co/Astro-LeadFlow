package com.astro.leadflow.shared.modules.integrations.webmotors

import com.astro.leadflow.shared.modules.integrations.webmotors.infra.dto.WmLeadPage
import com.astro.leadflow.shared.modules.integrations.webmotors.service.WebmotorsContactExtractor
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WebmotorsContactExtractorTest {

    private val mapper = jacksonObjectMapper()

    @Test
    fun `le contato no topo do detalhe`() {
        val c = WebmotorsContactExtractor.extract(
            mapper.readTree("""{"id":1,"nome":"Maria","email":"m@x.com","telefone":"(11) 98765-4321"}""")
        )
        assertEquals("Maria", c.name)
        assertEquals("m@x.com", c.email)
        assertEquals("(11) 98765-4321", c.phone)
    }

    @Test
    fun `le contato aninhado e ignora o nome do vendedor`() {
        val c = WebmotorsContactExtractor.extract(
            mapper.readTree(
                """{"vendedor":{"nome":"Vendedor"},"loja":{"nome":"Loja"},
                    "mensagem":{"nome":"Cliente","telefone":"11999998888"}}"""
            )
        )
        assertEquals("Cliente", c.name)
        assertEquals("11999998888", c.phone)
    }

    @Test
    fun `sem contato devolve nulos`() {
        val c = WebmotorsContactExtractor.extract(mapper.readTree("""{"vendedor":{"nome":"V"}}"""))
        assertNull(c.name)
        assertNull(c.phone)
    }

    @Test
    fun `listagem da collection desserializa e tolera campos novos`() {
        val page = mapper.readValue<WmLeadPage>(WebmotorsFixtures.LIST_JSON)
        val item = page.mensagens.single()
        assertEquals("460046", item.id)          // número virou String
        assertEquals(1, item.tipoLead)
        assertEquals("77", item.loja?.id)
        assertEquals("HB20", item.anuncio?.modelo)
        assertEquals(0, item.anuncio?.valor?.compareTo(java.math.BigDecimal("55000")))
        assertEquals("Vendedor Fulano", item.vendedor?.nome)
    }
}
