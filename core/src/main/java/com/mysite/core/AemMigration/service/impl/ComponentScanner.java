package com.mysite.core.AemMigration.service.impl;

import com.day.cq.wcm.commons.ResourceIterator;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mysite.core.AemMigration.service.ComponentListService;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.json.Json;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component(
        service = ComponentScanner.class,
        immediate = true
)
public class ComponentScanner {
    private static final Logger log = LoggerFactory.getLogger(ComponentScanner.class);

    private static final String COMPONENTS_PATH_SEGMENT = "/components/";
    private static final String DIALOG_NODE_NAME = "cq:dialog";
    private static final String JCR_CONTENT_NODE_NAME = "jcr:content";
    private static final String JCR_TITLE_PROPERTY = "jcr:title";
    private static final String COMPONENT_GROUP_PROPERTY = "componentGroup";
    private static final String SLING_RESOURCE_TYPE_PROPERTY = "sling:resourceType";
    private static final String REQUIRED_PROPERTY = "required";
    private static final String FIELD_DESCRIPTION_PROPERTY = "fieldDescription";
    private static final String DEFAULT_COMPONENT_GROUP = "General";
    private static final String CONTENT_XML_NODE_NAME = ".content.xml";
    private static final String CMP_CLASS_PREFIX = "cmp-";

    private static final String TEXTFIELD_RESOURCE_TYPE = "granite/ui/components/coral/foundation/form/textfield";
    private static final String NUMBERFIELD_RESOURCE_TYPE = "granite/ui/components/coral/foundation/form/numberfield";
    private static final String CHECKBOX_RESOURCE_TYPE = "granite/ui/components/coral/foundation/form/checkbox";

    private static final Map<String, String> FIELD_TYPE_MAPPING = new HashMap<>();

    static {
        FIELD_TYPE_MAPPING.put(TEXTFIELD_RESOURCE_TYPE, "String");
        FIELD_TYPE_MAPPING.put(NUMBERFIELD_RESOURCE_TYPE, "Number");
        FIELD_TYPE_MAPPING.put(CHECKBOX_RESOURCE_TYPE, "Boolean");
    }

    @Reference
    DialogToJsonConverter dialogToJsonConverter;

    @Reference
    ComponentListService componentListService;

    public List<Map<String, Object>> scanComponents(ResourceResolver resolver, String rootPath) {
        List<Map<String, Object>> components = new ArrayList<>();
        Resource rootResource = resolver.getResource(rootPath);

        if (rootResource != null) {
            try {
                scanComponentsRecursive(rootResource, components);
            } catch (RepositoryException e) {
                log.error("Error scanning components at path: {}", rootPath, e);
            }
        } else {
            log.warn("Root resource not found at path: {}", rootPath);
        }

        return components;
    }

    private void scanComponentsRecursive(Resource resource, List<Map<String, Object>> components)
            throws RepositoryException {
        if (isComponent(resource)) {
            components.add(createComponentMap(resource));
        }

        for (Resource child : resource.getChildren()) {
            scanComponentsRecursive(child, components);
        }
    }

    private boolean isComponent(Resource resource) {
        Node node = resource.adaptTo(Node.class);

        try {
            return node.getProperty("jcr:primaryType").getString().equals("cq:Component") && resource.getChild(DIALOG_NODE_NAME) != null;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }


    private Map<String, Object> createComponentMap(Resource resource) throws RepositoryException {
        Map<String, Object> componentMap = new HashMap<>();
        List<String> multiLst=new ArrayList<>();
        List<String> nestedMultiLst=new ArrayList<>();
        componentMap.put("componentPath", resource.getPath());
        componentMap.put("name", getComponentName(resource));
        componentMap.put("group", getComponentGroup(resource));
        componentMap.put("properties", dialogToJsonConverter.convertDialogToJson(resource.getPath(),resource));
        componentMap.put("htmlOutput", getComponentHtmlOutput(resource));
        componentMap.put("javascriptOutput",fetchClientLibsJs(resource));
        componentMap.put("cssOutput",fetchClientLibsCss(resource));
        String caller= "serviceImpl";

        Map<String, List<String>> result= componentListService.scanComponentsRecursive(resource,new ArrayList<>(),nestedMultiLst,caller);
        List<String> multifields = result.get("Multifield");
        List<String> nestedMultifields = result.get("NestedMultifield");
        if(multifields.size()==0){
            componentMap.put("Multifield",false);
        }
        else {
            componentMap.put("Multifield",true);
        }
        if(nestedMultifields.size()==0)
        {
            componentMap.put("NestedMultifield",false);
        }
        else{
            componentMap.put("NestedMultifield",true);
        }

        return componentMap;
    }

    private String getComponentName(Resource resource) {
        return (resource != null) ?
                resource.getValueMap().get(JCR_TITLE_PROPERTY, resource.getName()) :
                resource.getName();
    }

    private String getComponentGroup(Resource resource) {

        return (resource != null) ?
                resource.getValueMap().get(COMPONENT_GROUP_PROPERTY, DEFAULT_COMPONENT_GROUP) :
                DEFAULT_COMPONENT_GROUP;
    }

    private List<Map<String, Object>> getComponentProperties(Resource resource) {
        List<Map<String, Object>> properties = new ArrayList<>();
        Resource dialog = resource.getChild(DIALOG_NODE_NAME);
        if (dialog != null) {
            Resource content = dialog.getChild("content");
            if(content!=null){
                scanDialogProperties(content, properties);
            }
        }

        return properties;
    }

    private void scanDialogProperties(Resource dialogContent, List<Map<String, Object>> propertiesLst) {

//        Iterable<Resource> children = dialogContent.getChildren();
//       if(children.iterator().hasNext()){
//           Resource item = children.iterator().next();
//           String resourceType = item.getResourceType();
//           if("granite/ui/components/foundation/form/fieldset".equals(resourceType))
//           {
//               String multifieldName = item.getValueMap().get("name").toString();
//                Resource itemsNode = item.getChild("items");
//                if (itemsNode != null) {
//                    Resource columnNode = itemsNode.getChild("column");
//                    if (columnNode != null) {
//                        Resource fieldsNode = columnNode.getChild("items");
//                        if (fieldsNode != null) {
//                            for (Resource fieldItem : fieldsNode.getChildren()) {
//                                if(fieldItem.hasChildren()){
//                                    for(Resource nestedItem : fieldItem.getChildren()){
//                                        scanNestedDialog(nestedItem,propertiesLst);
//                                    }
//                                }
//                                // Process each field inside the multifield
//                                Map<String, Object> fieldProps = createPropertyMapforMultifield(fieldItem, multifieldName);
//                                if (fieldProps != null) {
//                                    propertiesLst.add(fieldProps);
//                                }
//                            }
//
//                        }
//                    }
//                }
//           }
//           Map<String, Object> fieldProps=createPropertyMap(item);
//           if(fieldProps!=null) {
//               propertiesLst.add(fieldProps);
//           }
//           scanDialogProperties(item,propertiesLst);
//       }





        for (Resource child : dialogContent.getChildren()) {

            String resourceType = child.getResourceType();
            String multifieldname= child.getValueMap().containsKey("name")?child.getValueMap().get("name").toString():"xyz";

            boolean alreadyExists = propertiesLst.stream()
                    .anyMatch(map -> multifieldname.equals(map.get("multifield")));

            if (alreadyExists) {
                continue;
            }

            if ("cq/gui/components/authoring/dialog/richtext".equals(resourceType)) {
                propertiesLst.add(createPropertyMap(child));
                continue; // Skip recursion for this node
            }
            if ("granite/ui/components/foundation/form/fieldset".equals(resourceType)) {
                // Handling multifield case
                String multifieldName = child.getValueMap().get("name").toString();
                Resource itemsNode = child.getChild("items");
                if (itemsNode != null) {
                    Resource columnNode = itemsNode.getChild("column");
                    if (columnNode != null) {
                        Resource fieldsNode = columnNode.getChild("items");
                        if (fieldsNode != null) {
                            for (Resource fieldItem : fieldsNode.getChildren()) {
                                if(fieldItem.hasChildren()){
                                    for(Resource nestedItem : fieldItem.getChildren()){
                                        scanNestedDialog(nestedItem,propertiesLst);
                                    }
                                }
                                // Process each field inside the multifield
                                Map<String, Object> fieldProps = createPropertyMapforMultifield(fieldItem, multifieldName);
                                if (fieldProps != null) {
                                    propertiesLst.add(fieldProps);
                                }
                            }

                        }
                    }
                }
                continue;
            } else {
                Map<String, Object> fieldProps=createPropertyMap(child);
                if(fieldProps!=null) {
                    propertiesLst.add(fieldProps);
                }
            }
            if (!"granite/ui/components/foundation/form/fieldset".equals(resourceType)) {
                scanDialogProperties(child, propertiesLst);
            }
        }
    }

    private void scanNestedDialog(Resource nestedItem, List<Map<String, Object>> propertiesLst) {
        String resourceType = nestedItem.getResourceType();
        if ("granite/ui/components/foundation/form/fieldset".equals(resourceType)) {
            // Handling multifield case
            String multifieldName = nestedItem.getValueMap().get("name").toString();
            Resource itemsNode = nestedItem.getChild("items");
            if (itemsNode != null) {
                Resource columnNode = itemsNode.getChild("column");
                if (columnNode != null) {
                    Resource fieldsNode = columnNode.getChild("items");
                    if (fieldsNode != null) {
                        for (Resource fieldItem : fieldsNode.getChildren()) {
                            if(fieldItem.hasChildren()){
                                for(Resource nestedfieldItem : fieldItem.getChildren()){
                                    scanNestedDialog(nestedfieldItem,propertiesLst);
                                }
                            }
                            // Process each field inside the multifield
                            Map<String, Object> fieldProps = createPropertyMapforMultifield(fieldItem, multifieldName);
                            if (fieldProps != null) {
                                propertiesLst.add(fieldProps);
                            }
                        }

                    }
                }
            }
        }
    }

    private boolean isPropertyField(Resource resource) {
        String slingResourceType = resource.getValueMap().get(SLING_RESOURCE_TYPE_PROPERTY, String.class);
        return slingResourceType != null && FIELD_TYPE_MAPPING.containsKey(slingResourceType);
    }

    private Map<String, Object> createPropertyMap(Resource field) {
        String nodeName = field.getName();
        Map<String, Object> propertyMap = new HashMap<>();
        if (!"items".equals(nodeName) && !"column".equals(nodeName)) {

            propertyMap.put("name", field.getName());
            propertyMap.put("type", field.getValueMap().containsKey("sling:resourceType") ? field.getValueMap().get("sling:resourceType") : "String");
            propertyMap.put("multifield","");
            propertyMap.put("fieldLabel", field.getValueMap().containsKey("fieldLabel") ? field.getValueMap().get("fieldLabel") : "");
            propertyMap.put("required", field.getValueMap().get(REQUIRED_PROPERTY, false));
            propertyMap.put("description", field.getValueMap().get(FIELD_DESCRIPTION_PROPERTY, ""));
            return propertyMap;

        }
        return null;
    }
    private Map<String, Object> createPropertyMapforMultifield(Resource field,String multifieldName) {
        String nodeName = field.getName();
        Map<String, Object> propertyMap = new HashMap<>();
        if (!"items".equals(nodeName) && !"column".equals(nodeName)) {

            propertyMap.put("name", field.getName());
            propertyMap.put("type", field.getValueMap().containsKey("sling:resourceType") ? field.getValueMap().get("sling:resourceType") : "String");
            propertyMap.put("multifield",multifieldName);
            propertyMap.put("fieldLabel", field.getValueMap().containsKey("fieldLabel") ? field.getValueMap().get("fieldLabel") : "");
            propertyMap.put("required", field.getValueMap().get(REQUIRED_PROPERTY, false));
            propertyMap.put("description", field.getValueMap().get(FIELD_DESCRIPTION_PROPERTY, ""));
            return propertyMap;

        }
        return null;
    }

    private String getFieldType(Resource field) {
        return FIELD_TYPE_MAPPING.getOrDefault(
                field.getValueMap().get(SLING_RESOURCE_TYPE_PROPERTY, ""),
                "String" // Default type if not found in mapping
        );
    }

    private String getComponentHtmlOutput(Resource resource) {
        Node node = resource.adaptTo(Node.class);
        try {
            if(node.getProperty("jcr:primaryType").getString().equals("cq:Component")){

                String htmlPath= node.getName()+".html";

                // String htmlFile = node.getName()+".html";
                Resource htmlResource= resource.getChild(htmlPath);

                if (htmlResource != null) {
                    try (InputStream is = htmlResource.adaptTo(InputStream.class)) {
                        if (is != null) {
                            return new BufferedReader(new InputStreamReader(is))
                                    .lines()
                                    .collect(Collectors.joining("\n"));
                        }
                    } catch (Exception e) {
                        return "Error reading HTML file: " + e.getMessage();
                    }
                }
//                if(node.hasNode(node.getNode(node.getName()+".html").getName())) {
//
//
//                    if (Objects.isNull(node.getNode(node.getName() + ".html"))) {
//                        log.error("html file is not present {}", node.getPath());
//                    } else if (Objects.nonNull(node.getNode(node.getName() + ".html"))) {
//                        Node htmlNode = node.getNode(node.getName() + ".html");
//                        Node htmlJcrNode = htmlNode.getNode("jcr:content");
//                        if (htmlJcrNode.hasProperty("jcr:data")) {
//                            InputStream inputStream = htmlJcrNode.getProperty("jcr:data").getBinary().getStream();
//                            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
//                        }
//                        //return "no Html file found at "+htmlFile;
//                    } else {
//                        log.error("Null Path {}", node.getNode(node.getName() + ".html"));
//                    }
//                }
            }
            else {
                log.error("its not component node {}",resource.getPath());
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
//        if(resource.getName().contains("html")){
//            resource.getChild("")
//        }
        // More robust approach would involve:
        // 1. Checking for an HTL script (.html file with the same name)
        // 2. If HTL exists, potentially parsing it (complex and might require a library)
        // 3. Alternatively, for a simpler approach, you could try to read a "sample.html"
        //    file next to the component definition if it exists.
        // 4. For this example, we'll keep a simplified structure.

        // Consider using a more descriptive class name based on the component's name
        // String componentClassName = CMP_CLASS_PREFIX + resource.getName().toLowerCase().replace(" ", "-");
//        return String.format(
//                "<div class=\"%s\">\n  \n</div>",
//                componentClassName,
//                getComponentName(resource)
//        );
        return "Component HTML not found";
    }

    JsonObject fetchClientLibsCss(Resource componentResource) {
        // Map<String, Object> clientLibsMap = new HashMap<>();
        JsonObject clientLibsMap=new JsonObject();
        Resource clientLibResource = componentResource.getChild("clientlib");
        if (clientLibResource != null) {
            // Fetch CSS files
            Resource cssFolder = clientLibResource.getChild("css");
            if (cssFolder != null) {
                for (Resource cssFile : cssFolder.getChildren()) {
                    if (cssFile.getName().endsWith(".css")) {
                        clientLibsMap.addProperty(cssFile.getName(), readFileContent(cssFile));
                    }
                }
            }

        }

        // Convert the Map to a JSON String
        return clientLibsMap;
    }
    JsonObject fetchClientLibsJs(Resource componentResource) {
        //  Map<String, Object> clientLibsMap = new HashMap<>();
        JsonObject clientLibsMap=new JsonObject();

        Resource clientLibResource = componentResource.getChild("clientlib");
        if (clientLibResource != null) {

            // Fetch JS files
            Resource jsFolder = clientLibResource.getChild("js");
            if (jsFolder != null) {
                for (Resource jsFile : jsFolder.getChildren()) {
                    if (jsFile.getName().endsWith(".js")) {
                        clientLibsMap.addProperty(jsFile.getName(), readFileContent(jsFile));
                    }
                }
            }
        }

        // Convert the Map to a JSON String
        return clientLibsMap;
    }

    private String readFileContent(Resource fileResource) {
        JsonObject jsonObject=new JsonObject();
        try {
            Node fileNode = fileResource.adaptTo(Node.class);
            if (fileNode != null && fileNode.hasNode("jcr:content")) {
                Node contentNode = fileNode.getNode("jcr:content");
                if (contentNode.hasProperty("jcr:data")) {
                    Property dataProperty = contentNode.getProperty("jcr:data");
                    Binary binary = dataProperty.getBinary();
                    InputStream inputStream = binary.getStream();

                    // Convert InputStream to String
                    StringBuilder content = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            content.append(line).append("\n");
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    //JsonObject contentObj=new JsonParser().parse(content.toString()).getAsJsonObject();
                    return content.toString();
                }
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return "";
    }
}
