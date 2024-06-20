package com.mysite.core.servlets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mysite.core.AI.service.ReGenAIService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.io.PrintWriter;

@Component(service= Servlet.class,
        property={"sling.servlet.methods="+ HttpConstants.METHOD_GET,
                "sling.servlet.methods="+HttpConstants.METHOD_POST,
                "sling.servlet.resourceTypes="+ "/apps/regenAI",
                "sling.servlet.extensions="+"json"})
public class RegenAiServlet extends SlingAllMethodsServlet {
    Logger logger= LoggerFactory.getLogger(RegenAiServlet.class);
    @Reference
    ReGenAIService reGenAIService;

    @Override
    protected  void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            PrintWriter out = response.getWriter();
            out.println("welcome");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            reGenAIService.getGenAi();
            response.getWriter().println("chat gpt response end");

        }

        catch (Exception e)
        {
            logger.error("Exception :{}",e);
        }
    }
}
