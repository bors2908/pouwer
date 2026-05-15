import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "ge.becrin.pouwer"

private val jvmLanguageVersion = JavaLanguageVersion.of(21)

allprojects {
    group = rootProject.group
    version = rootProject.version
}

fun Project.configureJvm21() {
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(jvmLanguageVersion)
        }
    }
}

subprojects {
    plugins.withId("java") {
        configureJvm21()
    }
    plugins.withId("java-library") {
        configureJvm21()
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_21)
            }
        }
    }

    tasks.withType<Copy>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
