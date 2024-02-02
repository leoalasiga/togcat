package com.als.togcat.engine.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/2/2 上午9:50
 */
@WebServlet(urlPatterns = "/login")
public class LoginServlet extends HttpServlet {

    static Map<String, String> users = new HashMap<>();

    static {
        users.put("bob", "bob123");
        users.put("alice", "alice123");
        users.put("root", "admin123");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String expectedPassword = users.get(username.toLowerCase());
        if (expectedPassword == null || !expectedPassword.equals(password)) {
            try (PrintWriter pw = resp.getWriter()) {
                String html = "<h1>Login Failed</h1>\n" +
                        "                        <p>Invalid username or password.</p>\n" +
                        "                        <p><a href=\"/\">Try again</a></p>";

            }
        } else {
            req.getSession().setAttribute("username", username);
            resp.sendRedirect("/");
        }
    }
}
