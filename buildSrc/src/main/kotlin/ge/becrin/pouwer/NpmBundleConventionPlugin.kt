package ge.becrin.pouwer

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class NpmBundleExtension @Inject constructor(objects: ObjectFactory) {
    val sourceDir: DirectoryProperty = objects.directoryProperty()
    val targetPath: Property<String> = objects.property(String::class.java)
}

abstract class NpmBuildBundleTask @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDir: DirectoryProperty

    @TaskAction
    fun runBuild() {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")

        execOperations.exec {
            workingDir = sourceDir.get().asFile
            commandLine = if (isWindows) {
                listOf("cmd", "/c", "npm run build")
            } else {
                listOf("sh", "-c", "npm run build")
            }
        }.assertNormalExitValue()
    }
}

abstract class NpmCopyBundleTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val destinationDir: DirectoryProperty

    @get:Input
    abstract val targetPath: Property<String>

    @TaskAction
    fun copyBundle() {
        project.copy {
            from(sourceDir.dir("dist").get().asFile)
            into(destinationDir.dir(targetPath.get()).get().asFile)
        }
    }
}

class NpmBundleConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("npmBundle", NpmBundleExtension::class.java)

        val npmBuildBundle = project.tasks.register("npmBuildBundle", NpmBuildBundleTask::class.java) {
            group = "build"
            description = "Build the browser bundle with npm."
            sourceDir.convention(extension.sourceDir)
        }

        val copyBundleDist = project.tasks.register("copyBundleDist", NpmCopyBundleTask::class.java) {
            group = "build"
            description = "Copy the browser bundle into the module resources."
            dependsOn(npmBuildBundle)
            sourceDir.convention(extension.sourceDir)
            destinationDir.convention(project.layout.buildDirectory.dir("resources/main"))
            targetPath.convention(extension.targetPath)
        }

        project.tasks.named("processResources") {
            finalizedBy(copyBundleDist)
        }

        project.tasks.named("jar", Jar::class.java) {
            dependsOn(copyBundleDist)
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }

        project.plugins.withId("com.gradleup.shadow") {
            project.tasks.named("shadowJar", Jar::class.java) {
                dependsOn(copyBundleDist)
                duplicatesStrategy = DuplicatesStrategy.INCLUDE
                archiveClassifier.set("")
            }

            project.tasks.named("jar") {
                enabled = false
            }

            project.tasks.named("assemble") {
                dependsOn("shadowJar")
            }
        }
    }
}
