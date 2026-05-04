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

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springdoc:springdoc-openapi-bom:3.0.1")
    }
}

dependencies {
    api(enforcedPlatform("org.springdoc:springdoc-openapi-bom:3.0.1"))
    api("org.springframework.hateoas:spring-hateoas")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")

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
