package com.mysite.core.AemMigration.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.mysite.core.AemMigration.service.ComponentListService;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.*;

@Component(service = ComponentListService.class,immediate = true)
public class ComponentListServiceImpl implements ComponentListService{
    Logger logger= LoggerFactory.getLogger(this.getClass());

    @Override
    public Map<String, List<String>> scanComponentsRecursive(Resource resource, List<String> multifieldList, List<String> componentsWithMultifield,String caller) throws RepositoryException {
        if (isComponent(resource)) {
            Resource dialogResource = resource.getChild("cq:dialog");

//            findMultifieldComponents(resource, componentsWithMultifield);
            processDialogElement(dialogResource,multifieldList,componentsWithMultifield);
        }
        if(caller.equals("Servlet")){
            for (Resource child : resource.getChildren()) {
                scanComponentsRecursive(child, multifieldList,componentsWithMultifield,caller);
            }
        }


        Map<String, List<String>> result = new HashMap<>();
        result.put("Multifield", multifieldList);
        result.put("NestedMultifield", componentsWithMultifield);
        return result;
        // return multifieldList;
    }

    private boolean isComponent(Resource resource) {
        Node node = resource.adaptTo(Node.class);

        try {
            return node.getProperty("jcr:primaryType").getString().equals("cq:Component") && resource.getChild("cq:dialog") != null;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private void processDialogElement(Resource resource,List<String> multifieldList,List<String> componentsWithMultifield) throws RepositoryException {
        try{
            JsonObject jsonObject = new JsonObject();
            //List<String> nestedMultifieldlst=new ArrayList<>();

            Node node = resource.adaptTo(Node.class);
            if (node == null) {
                logger.error("Exception{}",node);
            }


            String resourceType = getPropertyValue(resource, "sling:resourceType");

            if ("cq/gui/components/authoring/dialog".equals(resourceType)) {
                Resource contentResource = resource.getChild("content");
                if (contentResource != null) {
                    Resource itemsResource = contentResource.getChild("items");
                    Resource item = null;
                    if(Objects.nonNull(itemsResource.getChild("column")))
                    {
                        Resource column= itemsResource.getChild("column");
                        if(Objects.nonNull(column.getChild("items")))
                        {
                            item= column.getChild("items");

                        }
                    }
                    else {
                        item=itemsResource;
                    }
//                    Resource column= itemsResource.getChild("column");
//                    Resource item= column.getChild("items");
                    if (item != null) {
                        processChildren(item,multifieldList,componentsWithMultifield);
                    }
                }
            } else if ("granite/ui/components/coral/foundation/tabs".equals(resourceType)) {
                Resource itemsResource = resource.getChild("items");
                if (itemsResource != null) {
                    for (Resource tabResource : itemsResource.getChildren()) {

                        Resource tabItemsResource = tabResource.getChild("items");
                        if (tabItemsResource != null) {
                            processChildren(tabItemsResource,multifieldList,componentsWithMultifield);
                        }

                    }
                }

            }

            else if ("granite/ui/components/coral/foundation/form/multifield".equals(resourceType)) {
                // List<Object> stringList=new ArrayList<>();
                // JsonObject jsonObject1=new JsonObject();
                if(containsNestedMultifield(resource)){
                    if(resource.getPath().contains("/cq:dialog")){
                        String path= resource.getPath().substring(0,resource.getPath().indexOf("/cq:dialog"));
                        componentsWithMultifield.add(path);
                    }

//                    stringList.add(resource.getPath());
                    //componentsWithMultifield.add(jsonObject1);

                }
                if(resource.getPath().contains("/cq:dialog")){
                    String path= resource.getPath().substring(0,resource.getPath().indexOf("/cq:dialog"));
                    multifieldList.add(path);
                }



//            Resource fieldResource = resource.getChild("field");
//            if (fieldResource != null) {
//                Resource itemsResource = fieldResource.getChild("items");
//                if (itemsResource != null) {
//                    JsonArray nestedFields = new JsonArray();
//                    processChildren(itemsResource,componentsWithMultifield);
//                    componentsWithMultifield.add(resource.getPath());
//                }
//            }

            }
        }catch (Exception e)
        {
            logger.error("Exception in process dialog{}",e.getMessage());
        }


    }

    private void processChildren(Resource resource,List<String> multifieldList,List<String> componentsWithMultifield) throws RepositoryException {
        try{
            for (Resource childResource : resource.getChildren()) {
                String resourceType = getPropertyValue(childResource, "sling:resourceType");
                if ("granite/ui/components/coral/foundation/container".equals(resourceType)) {
                    Resource itemsResource = childResource.getChild("items");
                    if (itemsResource != null) {
                        processChildren(itemsResource,multifieldList,componentsWithMultifield);
                    }
                }
                else if ("granite/ui/components/coral/foundation/fixedcolumns".equals(resourceType)) {
                    Resource itemsResource = childResource.getChild("items");
                    if (itemsResource != null) {
                        processChildren(itemsResource, multifieldList, componentsWithMultifield);
                    }
                }
                else if ("granite/ui/components/coral/foundation/well".equals(resourceType)) {
                    Resource itemsResource = childResource.getChild("items");
                    if (itemsResource != null) {
                        processChildren(itemsResource, multifieldList, componentsWithMultifield);
                    }
                }else if ("granite/ui/components/coral/foundation/tabs".equals(resourceType)) {
                    processDialogElement(childResource,multifieldList,componentsWithMultifield);

                } else if (resourceType != null && resourceType.contains("/foundation/form/")) {
                    processDialogElement(childResource,multifieldList,componentsWithMultifield);
                }


            }
        }catch (Exception e)
        {
            logger.error("Exception in process children {}",e.getMessage());
        }

    }

    private String getPropertyValue(Resource resource, String propertyName) {
        return resource.getValueMap().get(propertyName, "");
    }

    private boolean containsNestedMultifield(Resource resource) {
        boolean hasMultifield = false;
        boolean hasNestedMultifield = false;
        // JsonArray jsonArray=new JsonArray();

//        for (Resource child : resource.getChildren()) {
        String resourceType = resource.getValueMap().get("sling:resourceType", String.class);
        if ("granite/ui/components/coral/foundation/form/multifield".equals(resourceType)) {
            hasMultifield = true;
            Resource fieldResource = resource.getChild("field").getChild("items").getChild("column").getChild("items");
            if (fieldResource != null && fieldResource.hasChildren()) {
                for (Resource fieldChild : fieldResource.getChildren()) {
                    String fieldType = fieldChild.getValueMap().get("sling:resourceType", String.class);
                    if ("granite/ui/components/coral/foundation/form/multifield".equals(fieldType)) {
                        hasNestedMultifield = true;
                        //nestedList.add(fieldChild.getPath());
                        //jsonObject.add("Fields",jsonArray);
                        break;
                    }
                }
            }
        }
        //}
        return hasNestedMultifield;
    }

    private boolean containsMultifield(Resource resource) {
        boolean hasMultifield = false;
        //boolean hasNestedMultifield = false;

//        for (Resource child : resource.getChildren()) {
        String resourceType = resource.getValueMap().get("sling:resourceType", String.class);
        if ("granite/ui/components/coral/foundation/form/multifield".equals(resourceType)) {
            hasMultifield = true;
            // Resource fieldResource = resource.getChild("field").getChild("items").getChild("column").getChild("items");
//            if (fieldResource != null && fieldResource.hasChildren()) {
//                for (Resource fieldChild : fieldResource.getChildren()) {
//                    String fieldType = fieldChild.getValueMap().get("sling:resourceType", String.class);
//                    if ("granite/ui/components/coral/foundation/form/multifield".equals(fieldType)) {
//                        hasNestedMultifield = true;
//                        break;
//                    }
//                }
//            }
        }
        //}
        return hasMultifield;
    }
}
