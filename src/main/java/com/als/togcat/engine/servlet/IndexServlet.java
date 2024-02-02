package com.als.togcat.engine.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/1/31 下午4:10
 */
@WebServlet(urlPatterns = "/")
public class IndexServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        String username = (String) session.getAttribute("username");
        String html;
       if (username == null) {
            html = "    <h1>Index Page</h1>\n" +
                    "                    <form method='post' action='/login'>\n" +
                    "                        <legend>Please Login</legend>\n" +
                    "                        <p>User Name: <input type='text' name='username'></p>\n" +
                    "                        <p>Password: <input type='password' name='password'></p>\n" +
                    "                        <p><button type='submit'>Login</button></p>\n" +
                    "                    </form>";
        } else {
            html = "<h1>Index Page</h1> <p>Welcome, {username}!</p> <p><a href='/logout'>Logout</a></p>".replace("{username}", username);
        }
        resp.setContentType("text/html");
        try (PrintWriter pw = resp.getWriter()) {
            pw.write(html);
        }
    }
}
