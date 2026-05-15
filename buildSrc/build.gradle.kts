plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("npmBundleConvention") {
            id = "ge.becrin.pouwer.npm-bundle"
            implementationClass = "ge.becrin.pouwer.NpmBundleConventionPlugin"
        }
    }
}
