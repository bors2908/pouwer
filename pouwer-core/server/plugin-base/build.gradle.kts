plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

description = "Base plugin module with common registration and controller logic"

dependencies {
    api(project(":pouwer-core:server:core-api"))
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.restclient)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.kotlin.logging.jvm)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.spring.boot.starter.validation)

    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(project(":pouwer-core:server:common-test"))
}

tasks.bootJar {
    enabled = false
}
