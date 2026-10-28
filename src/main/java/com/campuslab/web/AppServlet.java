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

@WebServlet("/app/*")
public class AppServlet extends HttpServlet {
    private final CampusLabService service = new CampusLabService();

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Map<String, Object> user = currentUser(request);
        if (user == null) { response.sendRedirect(request.getContextPath() + "/login"); return; }
        try {
            Database.initialize();
            if (request.getSession().getAttribute("csrfToken") == null) request.getSession().setAttribute("csrfToken", UUID.randomUUID().toString());
            Object flash = request.getSession().getAttribute("flash");
            if (flash != null) { request.setAttribute("flash", flash); request.getSession().removeAttribute("flash"); }
            String path = request.getPathInfo();
            String view = path == null || path.equals("/") ? "dashboard" : path.substring(1);
            boolean admin = "ADMIN".equals(user.get("role"));
            if ("users".equals(view) && !admin) { response.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
            switch (view) {
                case "dashboard" -> request.setAttribute("summary", service.summary(toLong(user.get("id")), admin));
                case "courses" -> request.setAttribute("courses", service.listCourses());
                case "labs" -> { request.setAttribute("labs", service.listLabs()); request.setAttribute("courses", service.listCourses()); }
                case "projects" -> { request.setAttribute("projects", service.listProjects()); request.setAttribute("users", service.listUsers()); }
                case "submissions" -> request.setAttribute("submissions", service.listSubmissions(toLong(user.get("id")), admin));
                case "users" -> request.setAttribute("users", service.listUsers());
                default -> { response.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
            }
            request.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(request, response);
        } catch (Exception ex) {
            getServletContext().log("Unable to load CampusLab page", ex);
            request.setAttribute("error", "The page could not be loaded. Check that MySQL Server is running and .env is configured.");
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        }
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        Map<String, Object> user = currentUser(request);
        if (user == null) { response.sendRedirect(request.getContextPath() + "/login"); return; }
        String expected = (String) request.getSession().getAttribute("csrfToken");
        if (expected == null || !expected.equals(request.getParameter("csrf"))) {
            flash(request, "Security token expired. Refresh the page and retry.");
            redirect(request, response, safeView(request.getPathInfo())); return;
        }
        boolean admin = "ADMIN".equals(user.get("role"));
        String view = safeView(request.getPathInfo());
        String action = request.getParameter("action");
        try {
            Database.initialize();
            if (action == null) throw new IllegalArgumentException("Choose an action and try again.");
            switch (action) {
                case "createCourse" -> {
                    requireAdmin(admin); service.createCourse(request.getParameter("code"), request.getParameter("name"), Integer.parseInt(request.getParameter("semester")));
                    flash(request, "Course added successfully."); view = "courses";
                }
                case "createLab" -> {
                    requireAdmin(admin); service.createLab(optionalLong(request.getParameter("courseId")), request.getParameter("title"), request.getParameter("description"), request.getParameter("dueDate"));
                    flash(request, "Lab created successfully."); view = "labs";
                }
                case "deleteLab" -> {
                    requireAdmin(admin); service.deleteLab(requiredLong(request.getParameter("id"))); flash(request, "Lab and its associated submissions deleted."); view = "labs";
                }
                case "createProject" -> {
                    requireAdmin(admin); service.createProject(request.getParameter("title"), request.getParameter("description"), request.getParameter("techStack"), optionalLong(request.getParameter("ownerId")));
                    flash(request, "Project created successfully."); view = "projects";
                }
                case "deleteProject" -> {
                    requireAdmin(admin); service.deleteProject(requiredLong(request.getParameter("id"))); flash(request, "Project deleted."); view = "projects";
                }
                case "updateProjectStatus" -> {
                    requireAdmin(admin); service.updateProjectStatus(requiredLong(request.getParameter("id")), request.getParameter("status")); flash(request, "Project status updated."); view = "projects";
                }
                case "submitLab" -> {
                    service.createSubmission(requiredLong(request.getParameter("labId")), toLong(user.get("id")), request.getParameter("submissionUrl"), request.getParameter("remarks"));
                    flash(request, "Submission saved. You can revise the URL later by submitting again."); view = "submissions";
                }
                case "reviewSubmission" -> {
                    requireAdmin(admin); service.reviewSubmission(requiredLong(request.getParameter("id")), request.getParameter("status"), request.getParameter("feedback"));
                    flash(request, "Submission review saved."); view = "submissions";
                }
                case "createUser" -> {
                    requireAdmin(admin); service.createUser(request.getParameter("name"), request.getParameter("email"), request.getParameter("password"), request.getParameter("role"));
                    flash(request, "User created successfully."); view = "users";
                }
                default -> throw new IllegalArgumentException("Unknown action.");
            }
        } catch (SecurityException ex) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN); return;
        } catch (Exception ex) {
            getServletContext().log("CampusLab action failed: " + action, ex);
            String message = ex instanceof IllegalArgumentException ? ex.getMessage() : "Action failed. Check your input and database connection.";
            flash(request, message == null ? "Action failed. Please retry." : message);
        }
        redirect(request, response, view);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object user = session.getAttribute("user");
        return user instanceof Map<?, ?> ? (Map<String, Object>) user : null;
    }
    private static void requireAdmin(boolean admin) { if (!admin) throw new SecurityException("Administrator access required."); }
    private static Long toLong(Object value) { return value == null ? null : Long.valueOf(value.toString()); }
    private static Long optionalLong(String value) { return value == null || value.isBlank() || "0".equals(value) ? null : requiredLong(value); }
    private static long requiredLong(String value) { try { return Long.parseLong(value); } catch (Exception ex) { throw new IllegalArgumentException("Select a valid record."); } }
    private static String safeView(String path) {
        if (path == null || path.equals("/")) return "dashboard";
        String v = path.substring(1);
        return switch (v) { case "dashboard", "courses", "labs", "projects", "submissions", "users" -> v; default -> "dashboard"; };
    }
    private static void flash(HttpServletRequest request, String message) { request.getSession().setAttribute("flash", message); }
    private static void redirect(HttpServletRequest request, HttpServletResponse response, String view) throws IOException {
        response.sendRedirect(request.getContextPath() + "/app/" + view);
    }
}
