plugins {
    java
}

group = "ru.itmo"

repositories {
    maven {
        url = uri("http://localhost:9001/repository/maven-public/")
        isAllowInsecureProtocol = true
    }
    mavenCentral()
}

subprojects {
    repositories {
        maven {
            url = uri("http://localhost:9001/repository/maven-public/")
            isAllowInsecureProtocol = true
        }
        mavenCentral()
    }

    tasks.withType<Copy>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
}
