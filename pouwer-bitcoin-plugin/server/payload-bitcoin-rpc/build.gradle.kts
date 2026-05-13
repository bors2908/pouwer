import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm")
    id("io.spring.dependency-management")
}

group = "ge.becrin.pouwer"

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.3")
    }
}

dependencies {
    implementation(project(":pouwer-core:server:core-api"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.bitcoinj:bitcoinj-core:0.16.3")
    compileOnly("org.pf4j:pf4j:3.14.1")
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

tasks.register<Exec>("npmBuildBundle") {
    val bundleDir = file("${rootProject.projectDir}/pouwer-bitcoin-plugin/browser/traefik-bitcoin")
    workingDir = bundleDir
    commandLine = if (System.getProperty("os.name").lowercase().contains("windows")) {
        listOf("cmd", "/c", "npm run build")
    } else {
        listOf("sh", "-c", "npm run build")
    }
}

tasks.register<Copy>("copyBundleDist") {
    dependsOn("npmBuildBundle")
    from(file("${rootProject.projectDir}/pouwer-bitcoin-plugin/browser/traefik-bitcoin/dist")) { into("static/traefik-bitcoin") }
    into(layout.projectDirectory.dir("src/main/resources"))
}

tasks.named("processResources") {
    dependsOn("copyBundleDist")
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from("src/main/resources") {
        include("plugin.properties")
        into("")
    }
}
