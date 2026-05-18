plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.jib)
    id("ge.becrin.pouwer.npm-bundle")
}

dependencies {
    implementation(project(":pouwer-core:server:common"))
    implementation(project(":pouwer-core:server:plugin-base"))
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.bitcoinj.core)
    implementation(libs.kotlin.logging.jvm)
}

npmBundleSource {
    sourceDir.set(rootProject.file("pouwer-bitcoin-plugin/browser/bitcoin"))
}

npmBundleResources {
    targetPath.set("static/bitcoin")
}

tasks.processResources {
    filesMatching("application.yml") {
        expand(mapOf("version" to project.version.toString()))
    }
}

springBoot {
    mainClass = "ge.becrin.pouwer.challenge.payload.bitcoin.BitcoinPluginApplicationKt"
}

jib {
    val projectName = "pouwer-bitcoin-plugin"
    val nexusUser = providers.gradleProperty("nexusUser").orNull
    val nexusPass = providers.gradleProperty("nexusPass").orNull

    setAllowInsecureRegistries(true)
    from {
        image = "eclipse-temurin:21-jre"
    }
    to {
        image = "${providers.gradleProperty("repo.url.docker.hosted").get()}/$projectName:${project.version}"
        auth {
            username = nexusUser
            password = nexusPass
        }
    }
    container.apply {
        mainClass = "ge.becrin.pouwer.challenge.payload.bitcoin.BitcoinPluginApplicationKt"
        ports = listOf("8084")
        creationTime = "USE_CURRENT_TIMESTAMP"
    }
}
