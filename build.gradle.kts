import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.testing.jacoco.tasks.JacocoReport

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

    if (providers.gradleProperty("enableCoverage").orNull == "true") {
        plugins.withId("java") {
        apply(plugin = "jacoco")

        tasks.withType<Test>().configureEach {
            finalizedBy("jacocoTestReport")
        }

        tasks.named<JacocoReport>("jacocoTestReport") {
            dependsOn(tasks.withType<Test>())

            reports {
                xml.required.set(true)
                html.required.set(true)
                csv.required.set(false)
            }

            val excludes = listOf(
                // Application bootstrap / wiring
                "**/ApplicationKt.class",
                "**/Application.class",
                "**/*Application.class",
                "**/*ApplicationKt.class",
                // Spring config / scheduler wiring (non-core business branches)
                "**/*Configuration.class",
                "**/*Config.class",
                "**/PowConfiguration.class",
                "**/Plugin*Registration.class",
                "**/PluginEvictionScheduler.class",
                // HTTP/controller glue and error mapping
                "**/*Controller.class",
                "**/*ErrorHandler*.class",
                "**/*Exception*.class",
                // DTO/model-only classes and generated wrappers
                "**/*Dto.class",
                "**/*Request.class",
                "**/*Response.class",
                "**/*Properties.class",
                "**/*Models*.class",
                "**/*PayloadModels*.class",
                // Third-party/system integration branches (out-of-scope for target metric)
                "**/*StratumTcpClient*.class",
                "**/*StratumSubmitService*.class",
                "**/*BitcoinRpcClient*.class",
                "**/*BitcoinBlockBuilder*.class",
                "**/*StaticPluginProxyService*.class",
                "**/*BitcoinBlockBuilder*.class"
            )

            classDirectories.setFrom(
                files(classDirectories.files.map { dir ->
                    fileTree(dir) {
                        exclude(excludes)
                    }
                })
            )
            executionData.setFrom(fileTree(layout.buildDirectory) {
                include("jacoco/*.exec", "outputs/unit_test_code_coverage/*.exec")
            })
        }
    }
    }
}
