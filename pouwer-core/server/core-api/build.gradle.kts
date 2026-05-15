plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlin.noarg)
}

dependencies {
    api(project(":pouwer-core:server:model"))
    api(platform(libs.spring.boot.bom))
    api(libs.spring.boot.starter.security)
    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.actuator)
    api(libs.commons.lang3)
    api(libs.jackson.datatype.jsr310)
    api(libs.jackson.module.kotlin)

    api(libs.spring.boot.starter.validation)

    api(libs.swagger.annotations)
    api(libs.kotlin.logging.jvm)
    api(libs.kotlin.stdlib.jdk8)
}
