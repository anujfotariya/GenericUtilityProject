package com.mysite.core.AemMigration.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mysite.core.AemMigration.service.ContentIteratorService;
import com.mysite.core.GenericBlogPackage.service.ResourceHelper;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component(service = ContentIteratorService.class,immediate = true)
public class ContentIteratorServiceImpl implements ContentIteratorService{

    @Reference
    ResourceHelper resourceHelper;
    Logger logger= LoggerFactory.getLogger(this.getClass());

    @Override
    public JsonObject getContent(Resource resource, String pagePath) {
        JsonObject finalJson = new JsonObject();
        JsonArray contentArray = new JsonArray();
        try{
            ResourceResolver resourceResolver=null;
            resourceResolver=resourceHelper.getResourceResolver();
            resource=resourceResolver.getResource(pagePath);
            for (Resource childPage : resource.getChildren()) {
                JsonObject pageObject = new JsonObject();
                JsonObject componentProps = new JsonObject();
                Resource jcrContent;
                if (childPage.getName().equals("jcr:content")) {
                    jcrContent = childPage;
                } else {

                    jcrContent = childPage.getChild("jcr:content");
                }
                if (jcrContent != null) {
                    // 1. Add page path
                    pageObject.addProperty("pagePath", childPage.getPath());

                    // 2. Add page-level properties
                    JsonObject pageProperties = new JsonObject();
                    jcrContent.getValueMap().forEach((k, v) -> pageProperties.addProperty(k, v.toString()));
                    pageObject.add("pageProperties", pageProperties);

                    // 3. Navigate to root/responsivegrid
                    Resource responsiveGrid = jcrContent.getChild("root/responsivegrid");
                    if (responsiveGrid != null) {
                        for (Resource component : responsiveGrid.getChildren()) {
                            JsonObject compProps = new JsonObject();
                            component.getValueMap().forEach((k, v) -> compProps.addProperty(k, v.toString()));
                            JsonObject nestedObject = new JsonObject();

                            Node node = component.adaptTo(Node.class);
                            if (node.hasNodes()) {
                                NodeIterator childNodes = node.getNodes();
                                while (childNodes.hasNext()) {
                                    Node childNode = childNodes.nextNode();
                                    String childName=childNode.getName();
                                    JsonObject childfieldNode=new JsonObject();
                                    JsonArray jsonArray=new JsonArray();
                                    JsonObject jsonObject = processNode(childNode, nestedObject,childName,childfieldNode);
                                    compProps.add("Fields", jsonObject);
                                }
                            }
                            // Handle multifield (example)
//                            if (compProps.has("multifield")) {
//                                // parse JSON stored string if needed or walk the child nodes
//                            }

                            // Add component name as key, properties as value
                            componentProps.add(component.getName(), compProps);
                        }
                    }

                    pageObject.add("components", componentProps);
                }


                contentArray.add(pageObject);
            }

            finalJson.add("Content", contentArray);

        }catch (Exception e)
        {
            logger.error("Exception in content service {}",e.getMessage());
        }




        return finalJson;
    }

    JsonObject processNode(Node node,JsonObject compJson,String childName,JsonObject childFieldnode) throws Exception {
        try {

            // Map to store properties of the current node
            String nodePath = node.getPath();
            String nodeName = node.getName(); // Get current node name
            JsonObject propertiesObj=new JsonObject();
            JsonObject fieldsObj=new JsonObject();
            JsonObject jsonObject=new JsonObject();
            // JsonArray jsonArray=new JsonArray();


            // Iterate through properties of the node
            PropertyIterator properties = node.getProperties();
            while (properties.hasNext()) {
                Property property = properties.nextProperty();

                if (property.isMultiple()) {
                    if (!property.getDefinition().isProtected()) {
                        // Handle multi-valued properties
                        Value[] values = property.getValues();
                        List<Value> updatedValues = new ArrayList<>();


                        for (Value val : values) {
                            String value = val.getString();
                            jsonObject.addProperty(property.getName(),value);
                        }
                    }
                } else {
                    // Handle single-valued properties
                    String value = property.getString();

                    jsonObject.addProperty(property.getName(), value);
                }
            }
            //jsonArray.add(jsonObject);
            //JsonObject childFieldnode=new JsonObject();
            childFieldnode.add(nodeName,jsonObject);
            //fieldsObj.add("fields",propertiesObj);



            // Process child nodes recursively
            if (node.hasNodes()) {
                NodeIterator childNodes = node.getNodes();
                while (childNodes.hasNext()) {
                    Node childNode = childNodes.nextNode();
                    processNode(childNode, compJson,childName,childFieldnode);
                }
            }
            fieldsObj.add("fields",childFieldnode);

            compJson.add(childName,fieldsObj);

        }catch (Exception e)
        {
            logger.error("Exception in process node {}",e.getMessage());
        }
        return compJson;
    }
}
