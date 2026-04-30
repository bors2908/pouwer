plugins {
    java
}

group = "ru.itmo"
version = "0.0.1-SNAPSHOT"

repositories {
    maven {
        url = uri("http://localhost:8081/repository/maven-public/")
        isAllowInsecureProtocol = true
    }
    mavenCentral()
}

subprojects {
    repositories {
        maven {
            url = uri("http://localhost:8081/repository/maven-public/")
            isAllowInsecureProtocol = true
        }
        mavenCentral()
    }

    tasks.withType<Copy>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
}
