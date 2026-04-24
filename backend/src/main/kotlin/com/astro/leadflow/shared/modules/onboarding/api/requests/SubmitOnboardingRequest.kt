package com.astro.leadflow.shared.modules.onboarding.api.requests

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SubmitOnboardingRequest(

    @field:NotBlank(message = "Nome da empresa é obrigatório")
    @field:Size(max = 255)
    val companyName: String,

    @field:Size(max = 255)
    val tradeName: String? = null,

    @field:NotBlank(message = "CNPJ é obrigatório")
    @field:Size(max = 20)
    val cnpj: String,

    @field:NotBlank(message = "Código do plano é obrigatório")
    @field:Size(max = 80)
    val planCode: String,

    @field:NotBlank(message = "Nome do responsável é obrigatório")
    @field:Size(max = 255)
    val ownerName: String,

    @field:NotBlank(message = "CPF do responsável é obrigatório")
    @field:Size(max = 14)
    val ownerCpf: String,

    @field:NotBlank(message = "E-mail do responsável é obrigatório")
    @field:Email(message = "E-mail inválido")
    @field:Size(max = 255)
    val ownerEmail: String,

    @field:NotBlank(message = "Telefone do responsável é obrigatório")
    @field:Size(max = 20)
    val ownerPhone: String,

    // Address (optional — filled via ViaCEP on the frontend)
    @field:Size(max = 10)
    val cep: String? = null,

    @field:Size(max = 255)
    val street: String? = null,

    @field:Size(max = 20)
    val number: String? = null,

    @field:Size(max = 100)
    val complement: String? = null,

    @field:Size(max = 100)
    val neighborhood: String? = null,

    @field:Size(max = 100)
    val city: String? = null,

    @field:Size(max = 2)
    val state: String? = null,

    // Referral attribution — populated from ?ref={code} query param by the frontend
    @field:Size(max = 32)
    val referralCode: String? = null
)
