import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar

plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.jib)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlin.noarg)
    alias(libs.plugins.shadow)
    id("ge.becrin.pouwer.maven-shadow-publish")
    id("ge.becrin.pouwer.npm-bundle")
}

val dockerRepoName: String = providers.gradleProperty("repo.url.docker.hosted").get()
val pagesDir: File = rootProject.file("pouwer-core/browser/widget-traefik/pages")

dependencies {
    implementation(project(":pouwer-core:server:common"))
    implementation(project(":pouwer-core:server:core-api"))
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.spring.cloud.bom))
    implementation(libs.spring.cloud.starter.openfeign)
    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.kotlinx.coroutines.core)

    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(project(":pouwer-core:server:common-test"))
}

npmBundleSource {
    sourceDir.set(rootProject.file("pouwer-core/browser"))
}

val copyPages: TaskProvider<Copy> = tasks.register<Copy>("copyPages") {
    from(pagesDir) {
        include("ban.html", "challenge.html")
    }
    into(layout.buildDirectory.dir("resources/main/static"))
}

tasks.processResources {
    finalizedBy(copyPages)
}

tasks.named("resolveMainClassName") {
    dependsOn(copyPages)
}

tasks.named("jar", Jar::class.java) {
    dependsOn(copyPages)
}

tasks.named("shadowJar", Jar::class.java) {
    dependsOn(copyPages)
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
