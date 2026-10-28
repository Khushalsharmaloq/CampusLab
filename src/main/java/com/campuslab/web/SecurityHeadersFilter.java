package com.campuslab.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Adds browser-side defense-in-depth headers to all web responses. */
@WebFilter("/*")
public final class SecurityHeadersFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse http) {
            http.setHeader("X-Content-Type-Options", "nosniff");
            http.setHeader("X-Frame-Options", "DENY");
            http.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            http.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
            http.setHeader("Content-Security-Policy", "default-src 'self'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'; object-src 'none'; img-src 'self' data:; style-src 'self'; script-src 'self'");
            if (request instanceof jakarta.servlet.http.HttpServletRequest httpRequest
                    && (httpRequest.getRequestURI().contains("/login") || httpRequest.getRequestURI().contains("/app/"))) {
                http.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                http.setHeader("Pragma", "no-cache");
            }
        }
        chain.doFilter(request, response);
    }
}
