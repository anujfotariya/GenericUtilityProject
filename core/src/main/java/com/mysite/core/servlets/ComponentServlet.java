package com.mysite.core.servlets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mysite.core.GenericBlogPackage.service.ResourceHelper;
import com.mysite.core.GenericBlogPackage.service.impl.ComponentScanner;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.util.List;
import java.util.Map;

@Component(service= Servlet.class,
        property={"sling.servlet.methods="+ HttpConstants.METHOD_GET,
                "sling.servlet.methods="+HttpConstants.METHOD_POST,
                "sling.servlet.resourceTypes="+ "/apps/component",
                "sling.servlet.extensions="+"json"})
public class ComponentServlet extends SlingAllMethodsServlet {

    @Reference
    ComponentScanner componentScanner;

    @Reference
    ResourceHelper resourceHelper;
    Logger logger= LoggerFactory.getLogger(this.getClass());

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
    {
        String rootPath= request.getParameter("rootPath");
        ResourceResolver resourceResolver=null;
        try{
            resourceResolver=resourceHelper.getResourceResolver();
            List<Map<String, Object>> maps = componentScanner.scanComponents(resourceResolver, rootPath);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String responseJson= gson.toJson(maps);
            response.setContentType("application/json");
            response.getWriter().println(responseJson);
        }catch (Exception e)
        {
           logger.error("Excpetion {}", e.getMessage());
        }
    }
}
