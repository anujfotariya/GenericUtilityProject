package com.mysite.core.AemMigration.service.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Component(
        service = DialogToJsonConverter.class,
        immediate = true
)
public class DialogToJsonConverter {

    private static final Logger logger = LoggerFactory.getLogger(DialogToJsonConverter.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public JsonObject convertDialogToJson(String componentPath, Resource componentResource) {
        JsonObject jsonResponse=new JsonObject();
        try {
            // Resource componentResource = resourceResolver.getResource(componentPath);
            if (componentResource != null) {
                Resource dialogResource = componentResource.getChild("cq:dialog");
                if (dialogResource != null) {
                    JsonObject jsonDialog = processDialogElement(dialogResource);
                    JsonObject root = new JsonObject();
                    root.addProperty("id", componentResource.getName()); // Using component name as ID
                    JsonArray fields = new JsonArray();
                    if (jsonDialog.has("fields")) {
                        jsonDialog.getAsJsonArray("fields").forEach(fields::add);
                    }
                    //root.add("fields", jsonDialog.getAsJsonArray("fields"));
                    if (jsonDialog.has("tabs")) {
                        JsonArray tabs = jsonDialog.getAsJsonArray("tabs");
                        for (int i = 0; i < tabs.size(); i++) {
                            JsonObject tab = tabs.get(i).getAsJsonObject();
                            if (tab.has("fields")) {
                                tab.getAsJsonArray("fields").forEach(fields::add);
                            }
                        }
                        // root.add("tabs", jsonDialog.getAsJsonArray("tabs"));
                    }
                    root.add("fields", fields);
                    return root;
                } else {
                    logger.warn("No cq:dialog found for component: {}", componentPath);
                    return new JsonObject();
                }
            } else {
                logger.warn("Component not found at path: {}", componentPath);
                return jsonResponse;
            }
        } catch (Exception e) {
            logger.error("Error converting dialog to JSON for component: {}", componentPath, e);
            return jsonResponse;
        }
    }

    private JsonObject processDialogElement(Resource resource) throws RepositoryException {
        JsonObject jsonObject = new JsonObject();
        Node node = resource.adaptTo(Node.class);
        if (node == null) {
            return jsonObject;
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

                if (item != null) {
                    JsonArray fields = new JsonArray();
                    JsonArray tabs = new JsonArray();
                    processChildren(item, fields, tabs);
                    if (fields.size() > 0) {
                        jsonObject.add("fields", fields);
                    }
                    if (tabs.size() > 0) {
                        jsonObject.add("tabs", tabs);

                    }
                }
            }
        } else if ("granite/ui/components/coral/foundation/tabs".equals(resourceType)) {
            JsonArray tabs = new JsonArray();
            Resource itemsResource = resource.getChild("items");
            if (itemsResource != null) {
                for (Resource tabResource : itemsResource.getChildren()) {
                    JsonObject tabObject = new JsonObject();
                    tabObject.addProperty("title", getPropertyValue(tabResource, "jcr:title"));
                    JsonArray fields = new JsonArray();
                    Resource tabItemsResource = tabResource.getChild("items");
                    if (tabItemsResource != null) {
                        processChildren(tabItemsResource, fields, new JsonArray());
                    }
                    tabObject.add("fields", fields);
                    tabs.add(tabObject);
                }
            }
            jsonObject.add("tabs", tabs);
        }
        else if ("cq/gui/components/authoring/dialog/richtext".equals(resourceType)) {
            JsonObject fieldObject = new JsonObject();
            fieldObject.addProperty("component", "richtext");
            fieldObject.addProperty("name", getPropertyValue(resource, "name").replace("./", ""));
            fieldObject.addProperty("label", getPropertyValue(resource, "fieldLabel"));
            fieldObject.addProperty("valueType", "String"); // Default value type for multifield

            return fieldObject;
        }
        else if ("granite/ui/components/coral/foundation/form/multifield".equals(resourceType)) {
            JsonObject fieldObject = new JsonObject();
            fieldObject.addProperty("component", "multiselect");
            fieldObject.addProperty("name", getPropertyValue(resource, "name").replace("./", ""));
            fieldObject.addProperty("label", getPropertyValue(resource, "fieldLabel"));
            fieldObject.addProperty("valueType", "String"); // Default value type for multifield

            Resource fieldResource = resource.getChild("field");
            if (fieldResource != null) {
                Resource itemsResource = fieldResource.getChild("items");
                if (itemsResource != null) {
                    JsonArray nestedFields = new JsonArray();
                    processChildren(itemsResource, nestedFields, new JsonArray());
                    fieldObject.add("fields", nestedFields);
                }
            }
            return fieldObject;
        }
        else if (resourceType != null && resourceType.contains("/foundation/form/")) {
            JsonObject fieldObject = new JsonObject();
            String componentName = resourceType.substring(resourceType.lastIndexOf("/") + 1);
            if ("pathbrowser".equals(componentName)) {
                String rootPath = getPropertyValue(resource, "rootPath");
                if (rootPath != null && rootPath.startsWith("/content/dam")) {
                    fieldObject.addProperty("component", "reference");
                } else {
                    fieldObject.addProperty("component", "aem-content");
                }
            } else if ("richtext".equals(componentName)) {
                fieldObject.addProperty("component", "richtext");
            } else if ("select".equals(componentName) || "multiselect".equals(componentName)) {
                fieldObject.addProperty("component", componentName);
                JsonArray options = new JsonArray();
                Resource itemsResource = resource.getChild("items");
                if (itemsResource != null) {
                    Iterator<Resource> optionIterator = itemsResource.listChildren();
                    while (optionIterator.hasNext()) {
                        Resource optionResource = optionIterator.next();
                        JsonObject optionObject = new JsonObject();
                        optionObject.addProperty("name", getPropertyValue(optionResource, "text"));
                        optionObject.addProperty("value", getPropertyValue(optionResource, "value"));
                        options.add(optionObject);
                    }
                }
                fieldObject.add("options", options);
            }
            else {
                fieldObject.addProperty("component", componentName);
            }
            fieldObject.addProperty("name", getPropertyValue(resource, "name").replace("./", ""));
            fieldObject.addProperty("label", getPropertyValue(resource, "fieldLabel"));
            fieldObject.addProperty("required", getBooleanPropertyValue(resource, "required"));
            if (resource.getValueMap().containsKey("max")) {
                fieldObject.addProperty("maxSize", getLongPropertyValue(resource, "max"));
            }
            // You might need to add more logic to determine valueType based on resourceType
            fieldObject.addProperty("valueType", "String"); // Default value type
            return fieldObject;
        }
        return jsonObject;

    }

    private void processChildren(Resource resource, JsonArray fields, JsonArray tabs) throws RepositoryException {
        for (Resource childResource : resource.getChildren()) {
            String resourceType = getPropertyValue(childResource, "sling:resourceType");
            if ("granite/ui/components/coral/foundation/container".equals(resourceType)) {
                Resource itemsResource = childResource.getChild("items");
                if (itemsResource != null) {
                    processChildren(itemsResource, fields, tabs);
                }
            }
            else if ("granite/ui/components/coral/foundation/fixedcolumns".equals(resourceType)) {
                Resource itemsResource = childResource.getChild("items");
                if (itemsResource != null) {
                    processChildren(itemsResource, fields, tabs);
                }
            }
            else if ("granite/ui/components/coral/foundation/well".equals(resourceType)) {
                Resource itemsResource = childResource.getChild("items");
                if (itemsResource != null) {
                    processChildren(itemsResource, fields, tabs);
                }
            }else if ("granite/ui/components/coral/foundation/tabs".equals(resourceType)) {
                JsonObject tabsObject = processDialogElement(childResource);
                if (tabsObject.has("tabs")) {
                    tabsObject.getAsJsonArray("tabs").forEach(tabs::add);


                }
            } else if (resourceType != null && resourceType.contains("/foundation/form/")) {
                fields.add(processDialogElement(childResource));
            }
            else if (resourceType != null && resourceType.contains("/dialog/richtext")) {
                fields.add(processDialogElement(childResource));
            }

        }
    }

    private String getPropertyValue(Resource resource, String propertyName) {
        return resource.getValueMap().get(propertyName, "");
    }
    private Boolean getBooleanPropertyValue(Resource resource, String propertyName) {
        return resource.getValueMap().get(propertyName, Boolean.FALSE);
    }

    private Long getLongPropertyValue(Resource resource, String propertyName) {
        return resource.getValueMap().get(propertyName, Long.class);
    }

//    public static void main(String[] args) {
//        String componentPath = "/apps/wknd/components/content/text";
//        ResourceResolver resourceResolver = null;
//
//        if (resourceResolver != null) {
//            DialogToJsonConverter converter = new DialogToJsonConverter();
//            String jsonOutput = converter.convertDialogToJson(componentPath, resourceResolver);
//            System.out.println(jsonOutput);
//        } else {
//            System.err.println("ResourceResolver is null. This code needs to run within an AEM request context.");
//        }
//    }
}
