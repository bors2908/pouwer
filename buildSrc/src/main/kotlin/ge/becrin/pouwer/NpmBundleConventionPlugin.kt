package ge.becrin.pouwer

import com.github.gradle.node.npm.task.NpmTask
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
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import javax.inject.Inject

abstract class NpmBundleSourceExtension @Inject constructor(objects: ObjectFactory) {
    val sourceDir: DirectoryProperty = objects.directoryProperty()
}

abstract class NpmBundleResourcesExtension @Inject constructor(objects: ObjectFactory) {
    val targetPath: Property<String> = objects.property(String::class.java)
}

abstract class NpmCopyBundleToResourcesTask : DefaultTask() {
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
        project.configureNodeExtension()

        val browserBundle = project.extensions.create("npmBundleSource", NpmBundleSourceExtension::class.java)
        val resourcesBundle = project.extensions.create("npmBundleResources", NpmBundleResourcesExtension::class.java)

        val buildBrowserBundle = project.registerBrowserBundleBuildTask(browserBundle)
        project.configureResourcePackaging(browserBundle, resourcesBundle, buildBrowserBundle)
    }
}

private fun Project.configureNodeExtension() {
    pluginManager.apply("com.github.node-gradle.node")

    val nodeVer = findProperty("node.version")?.toString()
    val npmVer = findProperty("npm.version")?.toString()

    extensions.configure<com.github.gradle.node.NodeExtension>("node") {
        version.set(nodeVer)
        npmVersion.set(npmVer)
        download.set(false)
    }
}

private fun Project.registerBrowserBundleBuildTask(
    browserBundle: NpmBundleSourceExtension,
): TaskProvider<NpmTask> = tasks.register("buildBrowserBundle", NpmTask::class.java) {
    group = "build"
    description = "Build the browser bundle."
    workingDir.set(browserBundle.sourceDir.get().asFile)
    args.set(listOf("run", "build"))
    dependsOn("npmInstall")
}

private fun Project.configureResourcePackaging(
    browserBundle: NpmBundleSourceExtension,
    resourcesBundle: NpmBundleResourcesExtension,
    buildBrowserBundle: TaskProvider<NpmTask>,
) {
    afterEvaluate {
        if (resourcesBundle.targetPath.isPresent) {
            val copyBundleToResources = registerCopyBundleToResourcesTask(browserBundle, resourcesBundle, buildBrowserBundle)
            wirePackagingTasks(copyBundleToResources)
        }
    }
}

private fun Project.registerCopyBundleToResourcesTask(
    browserBundle: NpmBundleSourceExtension,
    resourcesBundle: NpmBundleResourcesExtension,
    buildBrowserBundle: TaskProvider<NpmTask>,
): TaskProvider<NpmCopyBundleToResourcesTask> = tasks.register("copyBundleToResources", NpmCopyBundleToResourcesTask::class.java) {
    group = "build"
    description = "Copy the browser bundle to resources."
    dependsOn(buildBrowserBundle)
    sourceDir.convention(browserBundle.sourceDir)
    destinationDir.convention(layout.buildDirectory.dir("resources/main"))
    targetPath.convention(resourcesBundle.targetPath)
}

private fun Project.wirePackagingTasks(copyBundleToResources: TaskProvider<NpmCopyBundleToResourcesTask>) {
    tasks.named("processResources") {
        finalizedBy(copyBundleToResources)
    }

    tasks.named("resolveMainClassName") {
        dependsOn(copyBundleToResources)
    }

    tasks.named("jar", Jar::class.java) {
        dependsOn(copyBundleToResources)
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    plugins.withId("com.gradleup.shadow") {
        tasks.named("shadowJar", Jar::class.java) {
            dependsOn(copyBundleToResources)
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            archiveClassifier.set("")
        }

        tasks.named("jar") {
            enabled = false
        }

        tasks.named("assemble") {
            dependsOn("shadowJar")
        }
    }
}
