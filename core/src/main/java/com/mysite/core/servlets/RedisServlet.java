package com.mysite.core.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import redis.clients.jedis.UnifiedJedis;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

@Component(service = Servlet.class,property={"sling.servlet.methods="+ HttpConstants.METHOD_GET,
        "sling.servlet.resourceTypes="+ "/apps/redis",
        "sling.servlet.extensions="+"json"})
public class RedisServlet extends SlingAllMethodsServlet {
    @Override
    protected void doGet(SlingHttpServletRequest request,SlingHttpServletResponse response) throws ServletException, IOException {
        UnifiedJedis unifiedjedis = new UnifiedJedis(System.getenv().getOrDefault("REDIS_URL", "redis://127.0.0.1:6379"));
        response.getWriter().println("redis "+unifiedjedis.get("name"));
    }
}
