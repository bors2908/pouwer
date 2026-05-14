import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.publish.maven.MavenPublication

plugins {
    `java-library`
    kotlin("jvm")
    id("io.spring.dependency-management")
    id("maven-publish")
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
    compileOnly("org.pf4j:pf4j:3.15.0")
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
    into(layout.buildDirectory.dir("resources/main"))
}

tasks.named("processResources") {
    finalizedBy("copyBundleDist")
}

tasks.named<Jar>("jar") {
    dependsOn("copyBundleDist")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

publishing {
    repositories {
        maven {
            url = uri("http://localhost:9001/repository/maven-hosted/")
            credentials {
                username = findProperty("nexusUser") as String?
                password = findProperty("nexusPass") as String?
            }
            isAllowInsecureProtocol = true
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = project.name
        }
    }
}
