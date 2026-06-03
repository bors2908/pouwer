package ge.becrin.pouwer.challenge.payload.monero

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class StaticHeaderFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI

        if (path.endsWith(SCRIPT_NAME)) {
            response.setHeader(COOP_HEADER, COOP_VALUE)
            response.setHeader(COEP_HEADER, COEP_VALUE)
        }

        filterChain.doFilter(request, response)
    }

    companion object {
        private const val SCRIPT_NAME: String = "challenge-monero.js"

        private const val COOP_HEADER = "Cross-Origin-Opener-Policy"
        private const val COEP_HEADER = "Cross-Origin-Embedder-Policy"
        private const val COOP_VALUE = "same-origin"
        private const val COEP_VALUE = "require-corp"
    }
}

