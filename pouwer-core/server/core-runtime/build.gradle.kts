import org.gradle.api.tasks.Exec
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.jib)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlin.noarg)
}

val bundledPayloadsEnabled = providers
    .gradleProperty("challenge.bundledPayloads")
    .map(String::toBooleanStrictOrNull)
    .orElse(false)

val dockerRepoName = providers.gradleProperty("pouwerDockerRepoName").get()

dependencies {
    implementation(project(":pouwer-core:server:common"))
    implementation(project(":pouwer-core:server:core-api"))
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.spring.cloud.bom))
    implementation(libs.spring.cloud.starter.openfeign)
    implementation(libs.pf4j)
    implementation(libs.kotlin.stdlib)

    kapt(libs.mapstruct.processor)

    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(project(":pouwer-core:server:common-test"))

    if (bundledPayloadsEnabled.get()) {
        runtimeOnly(project(":pouwer-sha256-plugin:server:payload-sha256"))
        runtimeOnly(project(":pouwer-bitcoin-plugin:server:payload-bitcoin-rpc"))
        runtimeOnly(project(":pouwer-randomx-plugin:server:payload-monero-randomx"))
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
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

tasks.register<Exec>("npmBuildBrowser") {
    workingDir = rootProject.projectDir
    commandLine = if (System.getProperty("os.name").lowercase().contains("windows")) {
        listOf("cmd", "/c", "npm ci && npm run build")
    } else {
        listOf("sh", "-c", "npm ci && npm run build")
    }
}

jib {
    val projectName = "pouwer-server"
    val nexusUser = providers.gradleProperty("nexusUser").orNull
    val nexusPass = providers.gradleProperty("nexusPass").orNull

    setAllowInsecureRegistries(true)
    from {
        image = "eclipse-temurin:21-jre"
    }
    to {
        image = "$dockerRepoName/$projectName:${project.version}"
        auth {
            username = nexusUser
            password = nexusPass
        }
    }
    container.apply {
        mainClass = "ge.becrin.pouwer.ApplicationKt"
        ports = listOf("8082")
        creationTime = "USE_CURRENT_TIMESTAMP"
    }
}
