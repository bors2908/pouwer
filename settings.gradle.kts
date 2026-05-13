rootProject.name = "pouwer"

include("pouwer-core:server:common")
include("pouwer-core:server:model")
include("pouwer-core:server:core-api")
include("pouwer-core:server:core-runtime")
include("pouwer-core:server:openapi")
include("pouwer-core:server:common-test")

include("pouwer-sha256-plugin:server:payload-sha256")
include("pouwer-bitcoin-plugin:server:payload-bitcoin-rpc")
include("pouwer-randomx-plugin:server:payload-monero-randomx")

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
