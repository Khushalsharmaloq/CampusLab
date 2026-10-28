package com.campuslab.web;

import com.campuslab.config.Database;
import com.campuslab.service.CampusLabService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@WebServlet(urlPatterns = {"/login", "/logout"})
public class AuthServlet extends HttpServlet {
    private final CampusLabService service = new CampusLabService();

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if ("/logout".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        if (request.getSession(false) != null && request.getSession(false).getAttribute("user") != null) {
            response.sendRedirect(request.getContextPath() + "/app/dashboard"); return;
        }
        request.getSession(true).setAttribute("csrfToken", UUID.randomUUID().toString());
        try {
            Database.initialize();
        } catch (Exception ex) {
            getServletContext().log("CampusLab database initialisation failed", ex);
            request.setAttribute("error", "Database connection failed. Start MySQL and check DB_URL, DB_USER and DB_PASSWORD in .env.");
        }
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if ("/logout".equals(request.getServletPath())) {
            HttpSession logoutSession = request.getSession(false);
            String logoutToken = logoutSession == null ? null : (String) logoutSession.getAttribute("csrfToken");
            String supplied = request.getParameter("csrf");
            if (logoutToken == null || supplied == null || !java.security.MessageDigest.isEqual(
                    logoutToken.getBytes(java.nio.charset.StandardCharsets.UTF_8), supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            logoutSession.invalidate();
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        HttpSession existingSession = request.getSession(false);
        String expected = existingSession == null ? null : (String) existingSession.getAttribute("csrfToken");
        if (expected == null || !java.security.MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                String.valueOf(request.getParameter("csrf")).getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            request.setAttribute("error", "Your session expired. Refresh the page and try again.");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response); return;
        }
        try {
            Database.initialize();
            Map<String, Object> user = service.authenticate(request.getParameter("email"), request.getParameter("password"));
            if (user == null) {
                request.setAttribute("error", "Email or password is incorrect.");
                request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response); return;
            }
            HttpSession old = request.getSession(false);
            if (old != null) old.invalidate();
            HttpSession session = request.getSession(true);
            session.setAttribute("user", user);
            session.setAttribute("csrfToken", UUID.randomUUID().toString());
            response.sendRedirect(request.getContextPath() + "/app/dashboard");
        } catch (Exception ex) {
            getServletContext().log("CampusLab login failed", ex);
            request.setAttribute("error", "Could not sign in. Check the MySQL service and local database configuration.");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        }
    }
}
