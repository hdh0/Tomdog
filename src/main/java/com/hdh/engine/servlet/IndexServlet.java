package com.hdh.engine.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(urlPatterns = "/")
public class IndexServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        String username = (String) session.getAttribute("username");
        String html;
        if (username == null) {
            html = """
                    <h1>首页</h1>
                    <form method="post" action="/login">
                        <legend>登录</legend>
                        <p>用户名: <input type="text" name="username"></p>
                        <p>密码: <input type="password" name="password"></p>
                        <p><button type="submit">Login</button></p>
                    </form>
                    """;
        } else {
            html = """
                    <h1>首页</h1>
                    <p>Welcome, {username}!</p>
                    <p><a href="/logout">注销</a></p>
                    """.replace("{username}", username);
        }
        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter pw = resp.getWriter();
        pw.write(html);
        pw.close();
    }
}
