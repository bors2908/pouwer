package ru.itmo.enterprise

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

val DEFAULT_OBJECT_MAPPER: ObjectMapper = ObjectMapper().apply {
    setSerializationInclusion(JsonInclude.Include.NON_NULL)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    registerModule(KotlinModule.Builder().build())
    registerModule(JavaTimeModule())
}
