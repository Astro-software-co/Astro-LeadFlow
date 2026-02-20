package com.astro.leadflow.shared.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient

@Configuration
@ConditionalOnProperty(prefix = "app.aws", name = ["enabled"], havingValue = "true")
class SqsConfig(
    @Value("\${aws.region}") private val region: String
) {

    @Bean
    fun sqsClient(): SqsClient =
        SqsClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build()
}
