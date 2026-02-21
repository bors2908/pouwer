plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.allopen") apply true
    kotlin("plugin.noarg") apply true
}

group = "ru.itmo"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.1")
        mavenBom("org.springdoc:springdoc-openapi-bom:3.0.1")
    }
}

dependencies {
    api(project(":model"))

    api(enforcedPlatform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    api(enforcedPlatform("org.springframework.cloud:spring-cloud-dependencies:2025.1.1"))
    api(enforcedPlatform("org.springdoc:springdoc-openapi-bom:3.0.1"))
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-jdbc")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("org.springframework.hateoas:spring-hateoas")
    api("org.liquibase:liquibase-core")
    api("org.mapstruct:mapstruct:1.6.3")
    api("org.apache.commons:commons-lang3:3.20.0")
    api("io.hypersistence:hypersistence-utils-hibernate-63:3.15.2")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("org.springframework.kafka:spring-kafka")

    api("org.springframework.boot:spring-boot-starter-validation")

    api("io.swagger.core.v3:swagger-annotations:2.2.43")
    api("io.github.oshai:kotlin-logging-jvm:8.0.01")
    api(kotlin("stdlib-jdk8"))
}

noArg {
    annotation("jakarta.persistence.Entity")
}

tasks.test {
    useJUnitPlatform()
}
