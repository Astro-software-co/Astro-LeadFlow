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
    fun devBootstrapManagerRunner(
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
        @Value("\${app.dev.bootstrap-admin.admin-name:Manager Demo}")
        managerName: String,
        @Value("\${app.dev.bootstrap-admin.email:manager@astro.dev}")
        managerEmail: String,
        @Value("\${app.dev.bootstrap-admin.password:senha123}")
        managerPassword: String,
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

        val manager = userRepository.findByEmail(managerEmail)
            .map { existing ->
                existing.organization = organization
                existing.name = managerName
                existing.role = Role.MANAGER
                existing.status = UserStatus.ACTIVE
                existing.twoFactorEmailEnabled = false
                existing.twoFactorSmsEnabled = false
                existing.updatePassword(passwordEncoder.encode(managerPassword))
                userRepository.save(existing)
            }
            .orElseGet {
                userRepository.save(
                    User(
                        organization = organization,
                        name = managerName,
                        email = managerEmail,
                        passwordHash = passwordEncoder.encode(managerPassword),
                        role = Role.MANAGER,
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

        logger.info("Dev bootstrap manager ready: {} (org={}, userId={})", managerEmail, organizationSlug, manager.id)
    }

    @Bean
    fun devBootstrapAstroAdminRunner(
        userRepository: UserRepository,
        passwordEncoder: PasswordEncoder,
        @Value("\${app.dev.bootstrap-admin.enabled:true}")
        enabled: Boolean,
    ) = ApplicationRunner {
        if (!enabled) return@ApplicationRunner

        val astroAdminEmail = "admin@astro.dev"
        val legacyAstroAdminEmail = "astro.admin@astro.dev"
        val existingAstroAdmin = userRepository.findByEmail(astroAdminEmail)
        val legacyAstroAdmin = if (existingAstroAdmin.isPresent) null else userRepository.findByEmail(legacyAstroAdminEmail).orElse(null)

        if (existingAstroAdmin.isPresent || legacyAstroAdmin != null) {
            val existing = existingAstroAdmin.orElseGet { legacyAstroAdmin ?: error("Astro admin lookup failed.") }
            existing.organization = null
            existing.email = astroAdminEmail
            existing.name = "Astro Admin"
            existing.role = Role.ASTRO_ADMIN
            existing.status = UserStatus.ACTIVE
            existing.updatePassword(passwordEncoder.encode("senha123"))
            userRepository.save(existing)
        } else {
            userRepository.save(
                User(
                    organization = null,
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

    @Bean
    fun devBootstrapRoleUsersRunner(
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

        data class Seed(val email: String, val name: String, val role: Role)

        listOf(
            Seed("seller@astro.dev",  "Seller Demo",  Role.SELLER),
        ).forEach { seed ->
            userRepository.findByEmail(seed.email)
                .map { existing ->
                    existing.role   = seed.role
                    existing.status = UserStatus.ACTIVE
                    existing.updatePassword(passwordEncoder.encode("senha123"))
                    userRepository.save(existing)
                }
                .orElseGet {
                    userRepository.save(
                        User(
                            organization  = organization,
                            name          = seed.name,
                            email         = seed.email,
                            passwordHash  = passwordEncoder.encode("senha123"),
                            role          = seed.role,
                            status        = UserStatus.ACTIVE,
                        )
                    )
                }
            logger.info("Dev bootstrap {} ready: {}", seed.role, seed.email)
        }
    }
}
