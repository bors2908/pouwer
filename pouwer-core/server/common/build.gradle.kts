plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlin.noarg)
    `java-library`
}

dependencies {
    api(project(":pouwer-core:server:model"))
    api(platform(libs.spring.boot.bom))
    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.security)
    api(libs.spring.boot.starter.validation)
    api(libs.swagger.annotations)
    api(libs.spring.boot.starter.test)
    api(libs.spring.boot.starter.webmvc.test)
    api(libs.spring.security.test)
    api(libs.mockito.kotlin)

    api(platform(libs.junit.bom))
    api(libs.junit.jupiter)
    implementation(kotlin("stdlib"))
}
