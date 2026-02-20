package com.astro.leadflow.shared.config

import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
internal class RedisConfig {
    @Bean
    fun redisTemplate(): `fun`?

    init {
        val template: `val` = RedisTemplate < String
        val Any: `val`
        ()
        template.connectionFactory = connectionFactory

        val objectMapper: `val`? = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val jsonSerializer: `val`? = Jackson2JsonRedisSerializer(objectMapper, Any::Any::)
        java
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = jsonSerializer
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = jsonSerializer

        template.afterPropertiesSet()
        return template
    }
}