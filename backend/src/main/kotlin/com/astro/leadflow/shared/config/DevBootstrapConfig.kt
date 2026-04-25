package com.astro.leadflow.shared.config

import com.astro.leadflow.shared.modules.iam.domain.Organization
import com.astro.leadflow.shared.modules.iam.domain.OrganizationRepository
import com.astro.leadflow.shared.modules.iam.domain.Role
import com.astro.leadflow.shared.modules.iam.domain.User
import com.astro.leadflow.shared.modules.iam.domain.UserRepository
import com.astro.leadflow.shared.modules.iam.domain.UserStatus
import com.astro.leadflow.shared.modules.saas.domain.Subscription
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionRepository
import com.astro.leadflow.shared.modules.saas.domain.SubscriptionStatus
import com.astro.leadflow.shared.modules.saas.domain.PlanRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDate

@Configuration
@Profile("dev")
class DevBootstrapConfig {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun devBootstrapAdminRunner(
        organizationRepository: OrganizationRepository,
        userRepository: UserRepository,
        subscriptionRepository: SubscriptionRepository,
        planRepository: PlanRepository,
        passwordEncoder: PasswordEncoder,
        @Value("\${app.dev.bootstrap-admin.enabled:true}")
        enabled: Boolean,
        @Value("\${app.dev.bootstrap-admin.organization-name:Astro Demo}")
        organizationName: String,
        @Value("\${app.dev.bootstrap-admin.organization-slug:astro-demo}")
        organizationSlug: String,
        @Value("\${app.dev.bootstrap-admin.admin-name:Admin Demo}")
        adminName: String,
        @Value("\${app.dev.bootstrap-admin.email:admin@astro.dev}")
        adminEmail: String,
        @Value("\${app.dev.bootstrap-admin.password:senha123}")
        adminPassword: String,
    ) = ApplicationRunner {
        if (!enabled) {
            logger.info("Dev bootstrap admin disabled by configuration.")
            return@ApplicationRunner
        }

        val organization = organizationRepository.findBySlug(organizationSlug)
            .orElseGet {
                organizationRepository.save(
                    Organization(
                        name = organizationName,
                        slug = organizationSlug,
                    )
                )
            }

        val admin = userRepository.findByEmail(adminEmail)
            .map { existing ->
                existing.organization = organization
                existing.name = adminName
                existing.role = Role.ADMIN
                existing.status = UserStatus.ACTIVE
                existing.twoFactorEmailEnabled = false
                existing.twoFactorSmsEnabled = false
                existing.updatePassword(passwordEncoder.encode(adminPassword))
                userRepository.save(existing)
            }
            .orElseGet {
                userRepository.save(
                    User(
                        organization = organization,
                        name = adminName,
                        email = adminEmail,
                        passwordHash = passwordEncoder.encode(adminPassword),
                        role = Role.ADMIN,
                        status = UserStatus.ACTIVE,
                    )
                )
            }

        if (subscriptionRepository.findByOrganizationId(organization.id).isEmpty) {
            val fallbackPlan = planRepository.findAll()
                .minByOrNull { it.priceBrl }

            if (fallbackPlan != null) {
                subscriptionRepository.save(
                    Subscription(
                        organization = organization,
                        plan = fallbackPlan,
                        status = SubscriptionStatus.ACTIVE,
                        renewsAt = LocalDate.now().plusDays(30),
                    )
                )
            }
        }

        logger.info("Dev bootstrap admin ready: {} (org={}, userId={})", adminEmail, organizationSlug, admin.id)
    }

    @Bean
    fun devBootstrapAstroAdminRunner(
        organizationRepository: OrganizationRepository,
        userRepository: UserRepository,
        passwordEncoder: PasswordEncoder,
        @Value("\${app.dev.bootstrap-admin.enabled:true}")
        enabled: Boolean,
        @Value("\${app.dev.bootstrap-admin.organization-slug:astro-demo}")
        organizationSlug: String,
    ) = ApplicationRunner {
        if (!enabled) return@ApplicationRunner

        val organization = organizationRepository.findBySlug(organizationSlug).orElse(null) ?: return@ApplicationRunner

        val astroAdminEmail = "astro.admin@astro.dev"
        userRepository.findByEmail(astroAdminEmail)
            .map { existing ->
                existing.role = Role.ASTRO_ADMIN
                existing.status = UserStatus.ACTIVE
                existing.updatePassword(passwordEncoder.encode("senha123"))
                userRepository.save(existing)
            }
            .orElseGet {
                userRepository.save(
                    User(
                        organization = organization,
                        name = "Astro Admin",
                        email = astroAdminEmail,
                        passwordHash = passwordEncoder.encode("senha123"),
                        role = Role.ASTRO_ADMIN,
                        status = UserStatus.ACTIVE,
                    )
                )
            }

        logger.info("Dev bootstrap Astro Admin ready: {}", astroAdminEmail)
    }
}
