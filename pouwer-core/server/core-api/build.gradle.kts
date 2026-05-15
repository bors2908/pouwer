import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion

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
    api(libs.mapstruct)
    api(libs.commons.lang3)
    api(libs.jackson.datatype.jsr310)
    api(libs.jackson.module.kotlin)

    api(libs.spring.boot.starter.validation)

    api(libs.swagger.annotations)
    api(libs.kotlin.logging.jvm)
    api(libs.kotlin.stdlib.jdk8)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}
