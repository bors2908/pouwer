package ge.becrin.pouwer.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ge.becrin.pouwer.DEFAULT_OBJECT_MAPPER
import tools.jackson.databind.ObjectMapper

@Configuration
open class ApplicationConfiguration {
    @Bean
    open fun objectMapper(): ObjectMapper = DEFAULT_OBJECT_MAPPER
}
