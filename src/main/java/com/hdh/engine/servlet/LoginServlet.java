package com.hdh.engine.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

@WebServlet(urlPatterns = "/login")
public class LoginServlet extends HttpServlet {

    Map<String, String> users = Map.of(
            "hdh", "123456"
    );

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String expectedPassword = users.get(username.toLowerCase());
        if (expectedPassword == null || !expectedPassword.equals(password)) {
            PrintWriter pw = resp.getWriter();
            pw.write("""
                    <h1>登录失败</h1>
                    <p>用户名或密码错误.</p>
                    <p><a href="/">重新输入</a></p>
                    """);
            pw.close();
        } else {
            req.getSession().setAttribute("username", username);
            resp.sendRedirect("/");
        }
    }
}