package ge.becrin.pouwer

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

@SpringBootApplication
@EnableFeignClients(basePackages = ["ge.becrin.pouwer"])
@ConfigurationPropertiesScan(basePackages = ["ge.becrin.pouwer"])
open class Application
{
    @Bean
    fun logVersion(@Value($$"${spring.application.version}") version: String): ApplicationRunner {
        return ApplicationRunner {
            log.info { "core-runtime version $version" }
        }
    }

    @Bean
    fun extractPages(@Value($$"${pages.extract.path:}") extractPath: String): ApplicationRunner {
        return ApplicationRunner {
            if (extractPath.isBlank()) {
                return@ApplicationRunner
            }

            val targetRoot = Paths.get(extractPath)
            Files.createDirectories(targetRoot)

            val resolver = PathMatchingResourcePatternResolver()
            resolver.getResources("classpath*:static/**")
                .asSequence()
                .filter { it.isReadable && it.filename != null }
                .forEach { resource ->
                    val relativePath = resource.url.toString().substringAfter("/static/", resource.filename!!)
                    val destination = targetRoot.resolve(relativePath)
                    Files.createDirectories(destination.parent)
                    resource.inputStream.use { input ->
                        Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING)
                    }
                }

            log.info { "Extracted static pages to $extractPath" }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

private val log = KotlinLogging.logger {}
