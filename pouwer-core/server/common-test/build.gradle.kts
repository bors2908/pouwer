import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlin.noarg)
    `java-library`
}

dependencies {
    api(project(":pouwer-core:server:common"))
    api(platform(libs.spring.boot.bom))
    api(libs.spring.boot.starter.test)
    api(libs.spring.boot.starter.webmvc.test)
    api(libs.spring.security.test)
    api(libs.mockito.kotlin)

    api(platform(libs.junit.bom))
    api(libs.junit.jupiter)
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
