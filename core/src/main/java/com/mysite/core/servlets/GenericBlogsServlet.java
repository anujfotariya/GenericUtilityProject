package com.mysite.core.servlets;

import com.adobe.cq.dam.cfm.ContentFragmentException;
import com.day.cq.replication.ReplicationException;

import com.google.gson.*;
import com.mysite.core.GenericBlogPackage.service.GenericBlogService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component(service= Servlet.class,
        property={"sling.servlet.methods="+ HttpConstants.METHOD_GET,
                "sling.servlet.methods="+HttpConstants.METHOD_POST,
                "sling.servlet.resourceTypes="+ "/apps/genericBlogs",
                "sling.servlet.extensions="+"json"})
public class GenericBlogsServlet extends SlingAllMethodsServlet {


    @Reference
    GenericBlogService genericBlogService;

    Logger logger= LoggerFactory.getLogger(GenericBlogsServlet.class);


    @Override
    protected  void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            String test = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            JsonObject requestObject = (JsonObject) new JsonParser().parse(test);

            String path=requestObject.get("pagePath").getAsString();
            String limit=requestObject.get("limit").getAsString();
           String tag= requestObject.get("tags").getAsString();
           String[] tagsarray= tag.split(",");

           // String tagslst = Arrays.asList(String.valueOf(requestObject.get("tags").getAsJsonArray()));
            List<String> tags=Arrays.asList(tagsarray);
            PrintWriter out = response.getWriter();
//            out.println("welcome");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();


            String blogs=gson.toJson(genericBlogService.getBlogs(path,limit,tags));
            //  response.getWriter().println(json);
            response.getWriter().println(blogs);

        }
        //catch (ContentFragmentException e) {
//            throw new RuntimeException(e);
//        }
//        catch (ReplicationException e) {
//            throw new RuntimeException(e);
//        }
        catch (Exception e)
        {
            logger.error("Exception :{}",e);
        }
    }
}


