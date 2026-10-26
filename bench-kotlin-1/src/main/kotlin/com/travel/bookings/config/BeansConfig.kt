package com.travel.bookings.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.security.SecureRandom
import java.time.Duration
import java.util.Random

@Configuration
class BeansConfig {

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate =
        builder
            .setConnectTimeout(Duration.ofSeconds(3))
            .setReadTimeout(Duration.ofSeconds(10))
            .build()

    @Bean("voucherRandom")
    fun voucherRandom(): Random = SecureRandom()

    @Bean("quoteMapper")
    fun quoteMapper(): ObjectMapper {
        val validator = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.travel.bookings.model.")
            .allowIfSubType("java.util.")
            .allowIfSubType("java.math.BigDecimal")
            .build()
        return jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .activateDefaultTyping(validator, ObjectMapper.DefaultTyping.NON_FINAL)
    }
}
