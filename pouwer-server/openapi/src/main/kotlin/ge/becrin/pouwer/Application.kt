package ge.becrin.pouwer

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.servers.Server
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
open class Application {
    //Force server to allow Swagger to correctly work on aggregated view
    @Bean
    open fun openAPI(@Value($$"${gateway.host}") gatewayHost: String): OpenAPI {
        return OpenAPI()
            .servers(
                listOf(
                    Server()
                        .description("Gateway Local")
                        .url("$gatewayHost/"),
                    Server()
                        .description("Gateway K8s")
                        .url($$"${GATEWAY_HOST}"),
                    //TODO Temporary
                    Server()
                        .description("Remote Gateway K8s")
                        .url("http://192.168.1.2:8081")
                )
            )
    }

    private inline fun <reified T> mockComponent(): T {
        log.warn { "${T::class.simpleName} Mock is used. Only for OpenAPI generation purposes." }

        return mock(T::class.java)
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}
