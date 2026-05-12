package ge.becrin.pouwer.common.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ge.becrin.pouwer.DEFAULT_OBJECT_MAPPER

@Configuration
open class ApplicationConfiguration {
    @Bean
    open fun objectMapper(): ObjectMapper = DEFAULT_OBJECT_MAPPER
}
