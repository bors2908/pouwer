plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.allopen") apply true
    kotlin("plugin.noarg") apply true
}

group = "ge.becrin.pouwer"

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.1")
    }
}

dependencies {
    api(project(":pouwer-core:server:common"))
    api("org.springframework.boot:spring-boot-starter-test")
    api("org.springframework.boot:spring-boot-starter-webmvc-test")
    api("org.springframework.security:spring-security-test")
    api("org.mockito.kotlin:mockito-kotlin:6.2.3")

    api(platform("org.junit:junit-bom:6.0.3"))
    api("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
