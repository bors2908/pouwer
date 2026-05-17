plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.github.node-gradle:gradle-node-plugin:7.1.0")
}

gradlePlugin {
    plugins {
        create("npmBundleConvention") {
            id = "ge.becrin.pouwer.npm-bundle"
            implementationClass = "ge.becrin.pouwer.NpmBundleConventionPlugin"
        }
        create("mavenShadowPublishConvention") {
            id = "ge.becrin.pouwer.maven-shadow-publish"
            implementationClass = "ge.becrin.pouwer.MavenShadowPublishConventionPlugin"
        }
    }
}
