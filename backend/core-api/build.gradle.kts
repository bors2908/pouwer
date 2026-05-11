import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.allopen") apply true
    kotlin("plugin.noarg") apply true
    id("io.spring.dependency-management")
}

group = "ru.itmo"

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.3")
    }
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("org.pf4j:pf4j:3.13.0")
    api(kotlin("stdlib-jdk8"))
}

noArg {
    annotation("jakarta.persistence.Entity")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}
