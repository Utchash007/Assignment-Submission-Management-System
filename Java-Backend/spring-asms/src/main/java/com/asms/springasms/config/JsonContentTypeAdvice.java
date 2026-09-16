package com.asms.springasms.config;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * The .NET contract sends {@code application/json; charset=utf-8} on success responses;
 * Spring's Jackson converter omits the charset parameter. Set the literal header value
 * before the body is written. File downloads, empty responses, and problem+json error
 * bodies (which carry their own explicit content type) are untouched.
 */
@ControllerAdvice
public class JsonContentTypeAdvice implements ResponseBodyAdvice<Object> {

    private static final String JSON_UTF8 = "application/json; charset=utf-8";

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverter,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body != null && !(body instanceof Resource) && !(body instanceof byte[])
                && selectedContentType != null
                && selectedContentType.isCompatibleWith(MediaType.APPLICATION_JSON)
                && !selectedContentType.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                && response instanceof ServletServerHttpResponse servletResponse) {
            servletResponse.getServletResponse().setHeader(HttpHeaders.CONTENT_TYPE, JSON_UTF8);
        }
        return body;
    }
}
