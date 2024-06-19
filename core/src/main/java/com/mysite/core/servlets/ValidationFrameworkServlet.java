package com.mysite.core.servlets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mysite.core.ValidationFramework.service.ValidationFrameworkService;
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
                "sling.servlet.resourceTypes="+ "/apps/validationframework",
                "sling.servlet.extensions="+"json"})
public class ValidationFrameworkServlet extends SlingAllMethodsServlet {


    @Reference
    ValidationFrameworkService validationFrameworkService;

    Logger logger= LoggerFactory.getLogger(ValidationFrameworkServlet.class);


    @Override
    protected  void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {

            PrintWriter out = response.getWriter();
            out.println("welcome");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            String validationJson=gson.toJson(validationFrameworkService.getSingleFieldValues());

            response.getWriter().println(validationJson);


        }
        catch (Exception e)
        {
            logger.error("Exception :{}",e);
        }
    }
}
