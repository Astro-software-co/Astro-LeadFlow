import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.2.5"
	id("io.spring.dependency-management") version "1.1.7"
	id("jacoco")
	kotlin("jvm") version "1.9.23"
	kotlin("plugin.spring") version "1.9.23"
	kotlin("plugin.jpa") version "1.9.23"
	kotlin("plugin.noarg") version "1.9.23"
	kotlin("plugin.allopen") version "1.9.23"
}

group = "com.astro"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
	mavenCentral()
}

dependencies {

	// ── Spring Boot Core ─────────────────────────────────────────
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-webflux")

	// ── Redis ─────────────────────────────────────────────────────
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.apache.commons:commons-pool2")              // Pool de conexões Redis

	// ── Kotlin ────────────────────────────────────────────────────
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// ── JWT ───────────────────────────────────────────────────────
	implementation("io.jsonwebtoken:jjwt-api:0.12.5")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

	// ── Banco de Dados ────────────────────────────────────────────
	runtimeOnly("com.h2database:h2")
	runtimeOnly("org.postgresql:postgresql")
	implementation("org.flywaydb:flyway-core")

	// ── AWS SDK v2 ────────────────────────────────────────────────
	implementation(platform("software.amazon.awssdk:bom:2.25.0"))
	implementation("software.amazon.awssdk:sqs")
	implementation("software.amazon.awssdk:sns")
	implementation("software.amazon.awssdk:s3")
	implementation("software.amazon.awssdk:lexruntimev2")
	implementation("software.amazon.awssdk:bedrockruntime")

	// ── Spring Cloud AWS (SqsListener) ────────────────────────────
	implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:3.1.0"))
	implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")

	// ── Observabilidade ───────────────────────────────────────────
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	// Structured JSON logging (ativar no profile "prod"):
	// implementation("net.logstash.logback:logstash-logback-encoder:7.4")

	// ── OpenAPI / Swagger ─────────────────────────────────────────
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.4.0")

	// ── Testes ────────────────────────────────────────────────────
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("io.mockk:mockk:1.13.10")
	testImplementation("com.h2database:h2")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:postgresql")
	testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

// ── Configuração do Kotlin ────────────────────────────────────────
tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs += "-Xjsr305=strict"
		jvmTarget = "21"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

jacoco {
	toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required.set(true)
		html.required.set(true)
	}
}

tasks.jacocoTestCoverageVerification {
	dependsOn(tasks.test)
	violationRules {
		rule {
			element = "PACKAGE"
			includes = listOf(
				"com.astro.leadflow.shared.modules.chatbot.engine.mock",
				"com.astro.leadflow.shared.modules.chatbot.routing",
				"com.astro.leadflow.shared.modules.chatbot.session",
				"com.astro.leadflow.shared.modules.chatbot.usage",
				"com.astro.leadflow.shared.modules.monitoring.webhook"
			)
			limit {
				minimum = "0.80".toBigDecimal()
			}
		}
	}
}

tasks.check {
	dependsOn(tasks.jacocoTestCoverageVerification)
}

// ── Necessário para entidades JPA funcionarem com Kotlin ──────────
allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

// ── Necessário para que data classes JPA tenham construtor vazio ──
noArg {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}
