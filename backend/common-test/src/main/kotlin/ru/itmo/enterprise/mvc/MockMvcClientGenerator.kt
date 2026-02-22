package ru.itmo.enterprise.mvc

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockMultipartFile
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.util.CollectionUtils
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import ru.itmo.enterprise.common.controller.BaseController
import ru.itmo.enterprise.common.exception.DataConflictException
import ru.itmo.enterprise.common.exception.DataNotFoundException
import ru.itmo.enterprise.common.exception.InsufficientPermissionsException
import ru.itmo.enterprise.common.exception.InvalidRequestException
import ru.itmo.enterprise.common.exception.handler.response.BasicErrorResponse
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy

@Component
class MockMvcClientGenerator(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper
) {
    inline fun <reified T : BaseController> create(): T {
        return Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
            ControllerInvocationHandler(mockMvc, objectMapper)
        ) as T
    }

    class ControllerInvocationHandler(
        private val mockMvc: MockMvc,
        private val objectMapper: ObjectMapper
    ) : InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any {
            val result = performRequest(method, args)

            return when {
                ResponseEntity::class.java.isAssignableFrom(method.returnType) -> {
                    parseAsResponseEntity(method, result)
                }

                method.returnType == Void.TYPE -> {
                    result
                }

                else -> {
                    parseResponseBody(method, result.response.contentAsString)
                }
            }
        }

        private fun parseAsResponseEntity(method: Method, result: MvcResult): ResponseEntity<*> {
            val response = result.response

            if (response.contentType?.contains("multipart") == true && response.contentAsString.isBlank()) {
                return ResponseEntity
                    .status(result.response.status)
                    .body(response.contentAsString)
            }

            val responseBody: String? = response.contentAsString.takeIf { it.isNotBlank() }
            val bodyType: JavaType? = (method.genericReturnType as? ParameterizedType)
                ?.actualTypeArguments
                ?.firstOrNull()
                ?.let { objectMapper.typeFactory.constructType(it) }

            val headerMap: Map<String, List<String>> = response
                .headerNames
                .associateWith { response.getHeaders(it) }

            val responseEntity = ResponseEntity
                .status(result.response.status)
                .headers(HttpHeaders(CollectionUtils.toMultiValueMap(headerMap)))

            processExceptions(result)

            if (bodyType == null || responseBody == null) {
                return responseEntity.body(null)
            }

            val body: Any = objectMapper.readValue(
                responseBody,
                bodyType
            )

            return responseEntity.body(body)
        }

        private fun parseResponseBody(method: Method, content: String): Any {
            return if (content.isBlank()) {
                Unit
            } else {
                objectMapper.readValue(
                    content,
                    objectMapper.typeFactory.constructType(method.genericReturnType)
                )
            }
        }

        private fun performRequest(method: Method, args: Array<out Any>?): MvcResult {
            val mapping = method.getMappingAnnotation() ?: throw IllegalArgumentException("Method ${method.name} has no HTTP mapping")

            val path = buildPath(method, mapping)

            val request = createRequest(mapping.method, path, method, args)

            return mockMvc.perform(request).andReturn()
        }

        private fun Method.getMappingAnnotation(): RequestMappingInfo? {
            return when {
                isAnnotationPresent(GetMapping::class.java) ->
                    RequestMappingInfo(HttpMethod.GET, getAnnotation(GetMapping::class.java).value)

                isAnnotationPresent(PostMapping::class.java) ->
                    RequestMappingInfo(HttpMethod.POST, getAnnotation(PostMapping::class.java).value)

                isAnnotationPresent(PutMapping::class.java) ->
                    RequestMappingInfo(HttpMethod.PUT, getAnnotation(PutMapping::class.java).value)

                isAnnotationPresent(DeleteMapping::class.java) ->
                    RequestMappingInfo(HttpMethod.DELETE, getAnnotation(DeleteMapping::class.java).value)

                isAnnotationPresent(RequestMapping::class.java) -> {
                    val mapping = getAnnotation(RequestMapping::class.java)
                    RequestMappingInfo(mapping.method.firstOrNull()?.let { HttpMethod.valueOf(it.name) } ?: HttpMethod.GET,
                        mapping.value)
                }

                else -> null
            }
        }

        private fun buildPath(method: Method, mapping: RequestMappingInfo): String {
            val classPath = method.declaringClass.getAnnotation(RequestMapping::class.java)
                ?.value
                ?.firstOrNull()
                ?: ""

            val methodPath = mapping.paths.firstOrNull()
                ?: ""

            return "$classPath$methodPath"
        }

        private fun createRequest(
            method: HttpMethod,
            path: String,
            javaMethod: Method,
            args: Array<out Any>?
        ): MockHttpServletRequestBuilder {
            val resolvedPath = replacePathVariables(path, javaMethod, args)

            // Check if this is a multipart request (has RequestParam MultipartFile parameters)
            val isMultipart = javaMethod.parameters.any { param ->
                param.isAnnotationPresent(RequestParam::class.java) &&
                        param.type == MultipartFile::class.java
            }

            val requestBuilder: Any = if (isMultipart) {
                when (method) {
                    HttpMethod.POST -> multipart(resolvedPath)
                    HttpMethod.PUT -> multipart(HttpMethod.PUT, resolvedPath)
                    else -> throw IllegalArgumentException("Multipart requests only supported for POST")
                }
            } else {
                when (method) {
                    HttpMethod.GET -> get(resolvedPath)
                    HttpMethod.POST -> post(resolvedPath)
                    HttpMethod.PUT -> put(resolvedPath)
                    HttpMethod.DELETE -> delete(resolvedPath)
                    else -> get(resolvedPath)
                }
            }

            val request = requestBuilder as MockHttpServletRequestBuilder

            javaMethod.parameters.forEachIndexed { index, param ->
                when {
                    param.isAnnotationPresent(RequestBody::class.java) -> {
                        args?.get(index)?.let {
                            request.content(objectMapper.writeValueAsString(it))
                                .contentType(MediaType.APPLICATION_JSON)
                        }
                    }

                    param.isAnnotationPresent(RequestParam::class.java) -> {
                        val paramName = param.getAnnotation(RequestParam::class.java).value
                        when (val arg = args?.get(index)) {
                            is MultipartFile -> {
                                if (request is MockMultipartHttpServletRequestBuilder) {
                                    request.file(arg as MockMultipartFile)
                                }
                            }

                            else -> arg?.let {
                                request.param(paramName, it.toString())
                            }
                        }
                    }
                }
            }

            return request
        }

        private fun replacePathVariables(path: String, method: Method, args: Array<out Any>?): String {
            if (args == null) return path

            var resolvedPath = path
            method.parameters.forEachIndexed { index, param ->
                if (param.isAnnotationPresent(PathVariable::class.java)) {
                    val annotation = param.getAnnotation(PathVariable::class.java)
                    val paramName = annotation.value.takeIf { it.isNotBlank() } ?: param.name
                    val paramValue = args[index].toString()

                    resolvedPath = resolvedPath
                        .replace("{$param}", paramValue)
                        .replace("{$paramName}", paramValue)
                }
            }
            return resolvedPath
        }

        private fun processExceptions(result: MvcResult) {
            when (val status = HttpStatus.valueOf(result.response.status)) {
                HttpStatus.NOT_FOUND -> {
                    throw DataNotFoundException(getErrorMessage(result))
                }

                HttpStatus.BAD_REQUEST -> {
                    throw InvalidRequestException(getErrorMessage(result))
                }

                HttpStatus.CONFLICT -> {
                    throw DataConflictException(getErrorMessage(result))
                }

                HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED -> {
                    throw InsufficientPermissionsException(getErrorMessage(result))
                }

                HttpStatus.INTERNAL_SERVER_ERROR -> {
                    throw AssertionError(getErrorMessage(result))
                }

                HttpStatus.OK, HttpStatus.CREATED, HttpStatus.NO_CONTENT -> {
                    return
                }

                else -> {
                    throw AssertionError("Unknown HTTP status: " + status + " Message: " + getErrorMessage(result))
                }
            }
        }

        private fun getErrorMessage(result: MvcResult): String {
            val errorResponse = objectMapper.readValue(
                result.response.contentAsString,
                BasicErrorResponse::class.java
            )

            return errorResponse?.message
                ?: result.response.errorMessage.takeIf { it?.isNotBlank() ?: false }
                ?: "Unknown error"
        }

        private data class RequestMappingInfo(val method: HttpMethod, val paths: Array<String>)
    }
}
