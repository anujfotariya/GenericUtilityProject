package com.mysite.core.AemMigration.servlet;

import com.google.gson.JsonObject;
import com.mysite.core.AemMigration.service.ContentIteratorService;
import com.mysite.core.GenericBlogPackage.service.ResourceHelper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;

@Component(service= Servlet.class,
        property={"sling.servlet.methods="+ HttpConstants.METHOD_GET,
                "sling.servlet.methods="+HttpConstants.METHOD_POST,
                "sling.servlet.resourceTypes="+ "/apps/contentapi",
                "sling.servlet.extensions="+"json"})
public class ContenetIteratorServlet extends SlingAllMethodsServlet {
    Logger logger= LoggerFactory.getLogger(this.getClass());

    @Reference
    ResourceHelper resourceHelper;

    @Reference
    ContentIteratorService contentIteratorService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
    {
        try{
            ResourceResolver resourceResolver=null;
            resourceResolver=resourceHelper.getResourceResolver();
            String path= request.getParameter("pagePath");
            Resource resource = null;
            JsonObject jsonObject= contentIteratorService.getContent(resource,path);
            response.setContentType("application/json");
            response.getWriter().println(jsonObject);
        }
        catch (Exception e)
        {
            logger.error("Exception in content servlet {}",e.getMessage());
        }
    }
}
