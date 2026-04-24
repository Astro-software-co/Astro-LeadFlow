package com.astro.leadflow.shared.modules.onboarding.api

import com.astro.leadflow.shared.modules.onboarding.api.requests.SubmitOnboardingRequest
import com.astro.leadflow.shared.modules.onboarding.service.OnboardingApplicationService
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.UUID

class OnboardingControllerTest {

    @MockK
    lateinit var onboardingService: OnboardingApplicationService

    private lateinit var controller: OnboardingController

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        controller = OnboardingController(onboardingService)
    }

    @Test
    fun `submit should delegate to service and return 201`() {
        val request = SubmitOnboardingRequest(
            companyName = "AutoPrime Veículos Ltda",
            tradeName = null,
            cnpj = "12345678000190",
            planCode = "BASICO_MONTHLY",
            ownerName = "Carlos Oliveira",
            ownerCpf = "12345678900",
            ownerEmail = "carlos@autoprime.com",
            ownerPhone = "11999999999",
            referralCode = null
        )
        val expectedId = UUID.randomUUID()
        every { onboardingService.submit(request) } returns expectedId

        val response = controller.submit(request)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        verify(exactly = 1) { onboardingService.submit(request) }
    }

    @Test
    fun `submit with referralCode should pass referralCode to service`() {
        val request = SubmitOnboardingRequest(
            companyName = "AutoPrime Veículos Ltda",
            tradeName = null,
            cnpj = "12345678000190",
            planCode = "BASICO_MONTHLY",
            ownerName = "Carlos Oliveira",
            ownerCpf = "12345678900",
            ownerEmail = "carlos@autoprime.com",
            ownerPhone = "11999999999",
            referralCode = "REFCODE12345678"
        )
        every { onboardingService.submit(request) } returns UUID.randomUUID()

        controller.submit(request)

        verify { onboardingService.submit(match { it.referralCode == "REFCODE12345678" }) }
    }
}
