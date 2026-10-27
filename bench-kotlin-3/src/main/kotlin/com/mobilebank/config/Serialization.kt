package com.mobilebank.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object Serialization {
    private val scheduleTypes = BasicPolymorphicTypeValidator.builder()
        .allowIfSubType("com.mobilebank.model.")
        .allowIfSubType("java.util.")
        .allowIfSubType("java.math.BigDecimal")
        .allowIfSubType("java.time.")
        .build()

    val scheduleMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .activateDefaultTyping(scheduleTypes, ObjectMapper.DefaultTyping.NON_FINAL)
}
