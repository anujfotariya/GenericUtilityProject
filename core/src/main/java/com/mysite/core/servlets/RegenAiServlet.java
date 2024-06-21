package com.mysite.core.servlets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mysite.core.AI.service.ReGenAIService;
import com.mysite.core.GenericBlogPackage.service.ResourceHelper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Component(service= {Servlet.class},
        property={"sling.servlet.methods="+ HttpConstants.METHOD_GET,
                "sling.servlet.methods="+HttpConstants.METHOD_POST,
                "sling.servlet.resourceTypes="+ "/apps/regenAI",
                "sling.servlet.extensions="+"json"})


public class RegenAiServlet extends SlingAllMethodsServlet{
    Logger logger= LoggerFactory.getLogger(RegenAiServlet.class);
    @Reference
    ReGenAIService reGenAIService;


    @Reference
    ResourceHelper resourceHelper;

    @Reference
    JobManager jobManager;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        doPost(request,response);
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        try {
            String JOB_TOPIC = "com/mysite/core/servlets/RegenAiServlet";
            Map<String, Object> properties = new HashMap<>();
            jobManager.createJob(JOB_TOPIC).properties(properties).add();
            response.getWriter().write("ChatGpt Response Start");
            // Uncomment and use if needed
            // String message = reGenAIService.callAPIfg("test", "test");
            // reGenAIService.getGenAi();
            response.getWriter().write("ChatGpt Response End");
        } catch (Exception e) {
            logger.error("Error  : {}", e);
            response.getWriter().write("Sorry try again later!!");

        }
    }
}
