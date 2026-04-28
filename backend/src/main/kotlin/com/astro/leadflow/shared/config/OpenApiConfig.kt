package com.astro.leadflow.shared.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig(
    @Value("\${app.base-url:http://localhost:3001}") private val baseUrl: String,
    @Value("\${app.public-api-url:http://localhost:8080}") private val publicApiUrl: String
) {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Astro LeadFlow API")
                .description(
                    """
                    ## API do sistema SaaS de gestão de leads para concessionárias
                    
                    ### Autenticação
                    1. Use `POST /api/v1/auth/login` para obter um token JWT
                    2. Clique em **Authorize** (cadeado) e insira: `Bearer {seu_token}`
                    3. Todas as requisições autenticadas usarão o token automaticamente
                    
                    ### Multi-tenancy
                    Todos os dados são isolados por organização automaticamente via JWT.
                    Não é necessário enviar o `organizationId` nas requisições.
                    """.trimIndent()
                )
                .version("v1.0.0")
                .contact(
                    Contact()
                        .name("Astro LeadFlow")
                        .email("dev@astroleadflow.com")
                        .url("https://astroleadflow.com")
                )
                .license(
                    License()
                        .name("Proprietary")
                )
        )
        .addServersItem(Server().url(publicApiUrl).description("API pública configurada para webhooks e integrações"))
        .addServersItem(Server().url(baseUrl).description("Frontend/callbacks configurados"))
        .addServersItem(Server().url("http://localhost:8080").description("Local"))
        .addSecurityItem(SecurityRequirement().addList("Bearer Authentication"))
        .components(
            Components()
                .addSecuritySchemes(
                    "Bearer Authentication",
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            "Insira o token JWT obtido em POST /api/v1/auth/login. " +
                                    "Formato: Bearer {token}"
                        )
                )
        )
}
