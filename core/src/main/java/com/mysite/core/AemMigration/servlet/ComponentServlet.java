package com.mysite.core.AemMigration.servlet;

import com.google.gson.*;
import com.mysite.core.AemMigration.service.ComponentListService;
import com.mysite.core.AemMigration.service.impl.ComponentScanner;
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
import java.util.ArrayList;
import java.util.HashMap;
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
    @Reference
    ComponentListService componentListService;
    Logger logger= LoggerFactory.getLogger(this.getClass());

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
    {
        String rootPath= request.getParameter("rootPath");
        ResourceResolver resourceResolver=null;
        try{
            resourceResolver=resourceHelper.getResourceResolver();
            Resource rootResource = resourceResolver.getResource(rootPath);
            List<Map<String, Object>> maps = componentScanner.scanComponents(resourceResolver, rootPath);
            List<String> componentsWithMultifield =new ArrayList<>();
            JsonObject jsonObject=new JsonObject();
            Map<String,Object> objectMap=new HashMap<>();
            List<String> nestedMultiLst=new ArrayList<>();

            objectMap.put("Component Structure",maps);
            String caller="Servlet";

            Map<String, List<String>> result= componentListService.scanComponentsRecursive(rootResource,new ArrayList<>(),nestedMultiLst,caller);
            List<String> multifields = result.get("Multifield");
            List<String> nestedMultifields = result.get("NestedMultifield");
            objectMap.put("NestedMultifield",nestedMultifields);
            objectMap.put("Multifield",multifields);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
//            Gson gson1 = new Gson();
//            if(rootResource!=null)
//            {
//                componentListService.scanComponentsRecursive(rootResource,componentsWithMultifield);
//            }
//            JsonArray jsonArray = new JsonArray();
//            for (Object item : componentsWithMultifield) {
//
//                JsonElement element = gson1.toJsonTree(item); // Convert to JsonElement
//                jsonArray.add(element);
//            }
//            jsonObject.add("Field", jsonArray);
            String responseJson= gson.toJson(objectMap);
            jsonObject.addProperty("Component Structure",responseJson);
            response.setContentType("application/json");
            response.getWriter().println(responseJson);
            //response.getWriter().println(jsonObject);
        }catch (Exception e)
        {
            logger.error("Excpetion {}", e.getMessage());
        }
    }
}
