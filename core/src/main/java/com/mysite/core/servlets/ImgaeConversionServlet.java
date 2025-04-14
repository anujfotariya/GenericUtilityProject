package com.mysite.core.servlets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mysite.core.ImageConversion.service.ImageConverterService;
import com.mysite.core.ImageConversion.service.impl.ImageConvertDemo;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component(service= Servlet.class,
        property={"sling.servlet.methods="+ HttpConstants.METHOD_GET,
                "sling.servlet.methods="+HttpConstants.METHOD_POST,
                "sling.servlet.resourceTypes="+ "/apps/imageconversion",
                "sling.servlet.extensions="+"json"})
public class ImgaeConversionServlet extends SlingAllMethodsServlet {

    @Reference
    ImageConverterService imageConverterService;


    Logger logger= LoggerFactory.getLogger(this.getClass());

    @Override
    protected  void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            String test = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            JsonObject requestObject = (JsonObject) new JsonParser().parse(test);

            String folderPath=requestObject.get("folderPath").getAsString();
            imageConverterService.convertImagesInFolder(folderPath);
            response.getWriter().println("done");

        }

        catch (Exception e)
        {
            logger.error("Exception :{}",e);
        }
    }
}
