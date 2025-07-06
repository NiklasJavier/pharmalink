package de.jklein.pharmalink.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * Ein Filter, der den HTTP-Request-Body cacht, sodass er mehrmals gelesen werden kann.
 * Dies ist notwendig, damit Interceptoren oder andere Komponenten den Body lesen können,
 * auch nachdem der Controller ihn bereits konsumiert hat.
 * Ersetzt den (möglicherweise fehlenden) ContentCachingFilter in neueren Spring Boot Versionen.
 */
@Component
public class RequestCachingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean isAsync = request.isAsyncStarted();
        boolean isServletRequest = request.getDispatcherType().equals(jakarta.servlet.DispatcherType.REQUEST);
        boolean hasBody = "POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod()) || "PATCH".equalsIgnoreCase(request.getMethod());

        if (isServletRequest && !isAsync && hasBody) {
            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
            filterChain.doFilter(wrappedRequest, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}