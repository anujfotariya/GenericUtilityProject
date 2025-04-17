package com.mysite.core.AemMigration.service.impl;

import java.util.*;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import com.google.gson.*;
import com.mysite.core.AemMigration.configs.ComponentResourceTypeService;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = DialogToJsonConverter.class,
        immediate = true
)
public class DialogToJsonConverter {

    private static final Logger logger = LoggerFactory.getLogger(DialogToJsonConverter.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Reference
    ComponentResourceTypeService componentResourceTypeService;

    private Set<String> multifieldTypes;
    private Set<String> tabTypes;
    private Set<String> containerTypes;
    private Set<String> fixedColumnTypes;
    private Set<String> wellTypes;

    private static final String RESOURCE_TYPE = "sling:resourceType";
    private static final String DIALOG_RESOURCE_TYPE = "cq/gui/components/authoring/dialog";

    @Activate
    protected void activate() {
        multifieldTypes = componentResourceTypeService.getMultiFieldTypes();
        tabTypes = componentResourceTypeService.getTabTypes();
        containerTypes = componentResourceTypeService.getContainerTypes();
        fixedColumnTypes = componentResourceTypeService.getFixedColumnTypes();
        wellTypes = componentResourceTypeService.getWellTypes();
    }

    public JsonObject convertDialogToJson(String componentPath, Resource componentResource) {
        JsonObject jsonResponse = new JsonObject();

        if (componentResource == null) {
            logger.warn("Component not found at path: {}", componentPath);
            return jsonResponse;
        }

        try {
            Resource dialogResource = componentResource.getChild("cq:dialog");
            if (dialogResource == null) {
                logger.warn("No cq:dialog found for component: {}", componentPath);
                return jsonResponse;
            }

            JsonObject jsonDialog = processDialogElement(dialogResource);
            JsonObject root = new JsonObject();
            root.addProperty("id", componentResource.getName());

            JsonArray fields = new JsonArray();
            if (jsonDialog.has("fields")) {
                jsonDialog.getAsJsonArray("fields").forEach(fields::add);
            }
            if (jsonDialog.has("tabs")) {
                for (JsonElement tab : jsonDialog.getAsJsonArray("tabs")) {
                    JsonObject tabObj = tab.getAsJsonObject();
                    if (tabObj.has("fields")) {
                        tabObj.getAsJsonArray("fields").forEach(fields::add);
                    }
                }
            }

            root.add("fields", fields);
            return root;

        } catch (Exception e) {
            logger.error("Error converting dialog to JSON for component: {}", componentPath, e);
            return jsonResponse;
        }
    }

    private JsonObject processDialogElement(Resource resource) throws RepositoryException {
        JsonObject jsonObject = new JsonObject();
        Node node = resource.adaptTo(Node.class);
        if (node == null) return jsonObject;

        String resourceType = getPropertyValue(resource, RESOURCE_TYPE);

        if (DIALOG_RESOURCE_TYPE.equals(resourceType)) {
            return processDialogRoot(resource);
        }

        if (tabTypes.contains(resourceType)) {
            return processTabs(resource);
        }

        if (multifieldTypes.contains(resourceType)) {
            return processMultifield(resource);
        }

        if (resourceType != null && resourceType.contains("/foundation/form/")) {
            return processFormField(resource, resourceType);
        }

        if (resourceType != null && resourceType.contains("/dialog/richtext")) {
            return processRichText(resource);
        }

        return jsonObject;
    }

    private JsonObject processDialogRoot(Resource resource) throws RepositoryException {
        JsonObject jsonObject = new JsonObject();
        Resource contentResource = resource.getChild("content");
        if (contentResource == null) return jsonObject;

        Resource itemsResource = Optional.ofNullable(contentResource.getChild("items"))
                .map(items -> items.getChild("column"))
                .map(col -> col.getChild("items"))
                .orElse(contentResource.getChild("items"));

        if (itemsResource != null) {
            JsonArray fields = new JsonArray();
            JsonArray tabs = new JsonArray();
            processChildren(itemsResource, fields, tabs);

            if (fields.size() > 0) jsonObject.add("fields", fields);
            if (tabs.size() > 0) jsonObject.add("tabs", tabs);
        }

        return jsonObject;
    }

    private JsonObject processTabs(Resource resource) throws RepositoryException {
        JsonObject jsonObject = new JsonObject();
        JsonArray tabs = new JsonArray();
        Resource itemsResource = resource.getChild("items");

        if (itemsResource != null) {
            for (Resource tab : itemsResource.getChildren()) {
                JsonObject tabObject = new JsonObject();
                tabObject.addProperty("title", getPropertyValue(tab, "jcr:title"));

                JsonArray fields = new JsonArray();
                Resource tabItems = tab.getChild("items");
                if (tabItems != null) {
                    processChildren(tabItems, fields, new JsonArray());
                }
                tabObject.add("fields", fields);
                tabs.add(tabObject);
            }
        }

        jsonObject.add("tabs", tabs);
        return jsonObject;
    }

    private JsonObject processMultifield(Resource resource) throws RepositoryException {
        JsonObject fieldObject = createFieldBase(resource, "multiselect", "String");

        Resource fieldRes = resource.getChild("field");
        if (fieldRes != null) {
            Resource items = fieldRes.getChild("items");
            if (items != null) {
                JsonArray nestedFields = new JsonArray();
                processChildren(items, nestedFields, new JsonArray());
                fieldObject.add("fields", nestedFields);
            }
        }

        return fieldObject;
    }

    private JsonObject processFormField(Resource resource, String resourceType) {
        String type = resourceType.substring(resourceType.lastIndexOf("/") + 1);
        String component;
        switch (type) {
            case "pathbrowser":
            case "pathfield":
                String rootPath = getPropertyValue(resource, "rootPath");
                component = (rootPath != null && rootPath.startsWith("/content/dam")) ? "reference" : "aem-content";
                break;
            case "richtext":
            case "textarea":
                component = "richtext";
                break;
            case "textfield":
                component = "text";
                break;
            case "numberfield":
                component = "number";
                break;
            case "checkbox":
                component = "Boolean";
                break;
            case "select":
            case "multiselect":
                component = "select";
                break;
            case "radiogroup":
                component = "radio-group";
                break;
            default:
                component = type;
                break;
        }

        String valueType;
        switch (component) {
            case "Boolean":
                valueType = "Boolean";
                break;
            case "number":
                valueType = "number";
                break;
            default:
                valueType = "String";
                break;
        }

        JsonObject fieldObject = createFieldBase(resource, component, valueType);

        if (component.equals("select") || component.equals("radio-group")) {
            fieldObject.add("options", extractOptions(resource));
        }

        return fieldObject;
    }

    private JsonObject processRichText(Resource resource) {
        return createFieldBase(resource, "richtext", "String");
    }

    private void processChildren(Resource resource, JsonArray fields, JsonArray tabs) throws RepositoryException {
        for (Resource child : resource.getChildren()) {
            String resourceType = getPropertyValue(child, RESOURCE_TYPE);

            if (containerTypes.contains(resourceType)
                    || fixedColumnTypes.contains(resourceType)
                    || wellTypes.contains(resourceType)) {

                Optional.ofNullable(child.getChild("items"))
                        .ifPresent(items -> {
                            try {
                                processChildren(items, fields, tabs);
                            } catch (RepositoryException e) {
                                logger.error("Error processing container children", e);
                            }
                        });

            } else if (tabTypes.contains(resourceType)) {
                JsonObject tabObj = processDialogElement(child);
                tabObj.getAsJsonArray("tabs").forEach(tabs::add);

            } else if (resourceType != null &&
                    (resourceType.contains("/foundation/form/") || resourceType.contains("/dialog/richtext"))) {
                fields.add(processDialogElement(child));
            }
        }
    }

    private JsonObject extractOptions(Resource resource) {
        JsonArray options = new JsonArray();
        Resource items = resource.getChild("items");

        if (items != null) {
            for (Resource option : items.getChildren()) {
                JsonObject opt = new JsonObject();
                opt.addProperty("name", getPropertyValue(option, "text"));
                opt.addProperty("value", getPropertyValue(option, "value"));
                options.add(opt);
            }
        }

        JsonObject optionsWrapper = new JsonObject();
        optionsWrapper.add("options", options); // wrapping array in an object with key "options"
        return optionsWrapper;
    }

    private JsonObject createFieldBase(Resource resource, String component, String valueType) {
        JsonObject obj = new JsonObject();
        obj.addProperty("component", component);
        obj.addProperty("name", getPropertyValue(resource, "name").replace("./", ""));
        obj.addProperty("label", getPropertyValue(resource, "fieldLabel"));
        obj.addProperty("valueType", valueType);
        obj.addProperty("required", getBooleanPropertyValue(resource, "required"));

        if (resource.getValueMap().containsKey("max")) {
            obj.addProperty("maxSize", getLongPropertyValue(resource, "max"));
        }

        return obj;
    }

    private String getPropertyValue(Resource resource, String propertyName) {
        return resource.getValueMap().getOrDefault(propertyName, "").toString();
    }

    private Boolean getBooleanPropertyValue(Resource resource, String propertyName) {
        return (Boolean) resource.getValueMap().getOrDefault(propertyName, Boolean.FALSE);
    }

    private Long getLongPropertyValue(Resource resource, String propertyName) {
        return (Long) resource.getValueMap().getOrDefault(propertyName, 0L);
    }

}
