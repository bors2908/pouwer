rootProject.name = "pouwer"

include("common")
include("model")

include("core-api")
include("core-runtime")
include("payloads")
include("payloads:payload-sha256")
include("payloads:payload-bitcoin-rpc")
include("payloads:payload-monero-randomx")

include("openapi")

include("common-test")

pluginManagement {
    plugins {
        kotlin("jvm") version "2.3.10" apply false
        kotlin("kapt") version "2.3.10" apply false
        kotlin("plugin.allopen") version "2.3.10" apply false
        kotlin("plugin.noarg") version "2.3.10" apply false
        id("org.springframework.boot") version "4.0.3" apply false
        id("io.spring.dependency-management") version "1.1.7" apply false
        id("org.springdoc.openapi-gradle-plugin") version "1.9.0" apply false
    }

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
