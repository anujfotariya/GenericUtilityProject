package com.mysite.core.GenericBlogPackage.service.impl;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private Map<String, Object> createComponentMap(Resource resource) {
        Map<String, Object> componentMap = new HashMap<>();
        componentMap.put("componentPath", resource.getPath());
        componentMap.put("name", getComponentName(resource));
        componentMap.put("group", getComponentGroup(resource));
        componentMap.put("properties", getComponentProperties(resource));
        componentMap.put("htmlOutput", getComponentHtmlOutput(resource));

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
            if (content != null) {
                scanDialogProperties(content, properties);
            }
        }

        return properties;
    }

    private void scanDialogProperties(Resource dialogContent, List<Map<String, Object>> properties) {
        for (Resource child : dialogContent.getChildren()) {
            if (isPropertyField(child)) {
                properties.add(createPropertyMap(child));
            }

            // Recursively scan nested structures
            for (Resource grandChild : child.getChildren()) {
                scanDialogProperties(grandChild, properties);
            }
        }
    }

    private boolean isPropertyField(Resource resource) {
        String slingResourceType = resource.getValueMap().get(SLING_RESOURCE_TYPE_PROPERTY, String.class);
        return slingResourceType != null && FIELD_TYPE_MAPPING.containsKey(slingResourceType);
    }

    private Map<String, Object> createPropertyMap(Resource field) {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("name", field.getName());
        propertyMap.put("type", getFieldType(field));
        propertyMap.put("required", field.getValueMap().get(REQUIRED_PROPERTY, false));
        propertyMap.put("description", field.getValueMap().get(FIELD_DESCRIPTION_PROPERTY, ""));

        return propertyMap;
    }

    private String getFieldType(Resource field) {
        return FIELD_TYPE_MAPPING.getOrDefault(
                field.getValueMap().get(SLING_RESOURCE_TYPE_PROPERTY, ""),
                "String" // Default type if not found in mapping
        );
    }

    private String getComponentHtmlOutput(Resource resource) {
        // More robust approach would involve:
        // 1. Checking for an HTL script (.html file with the same name)
        // 2. If HTL exists, potentially parsing it (complex and might require a library)
        // 3. Alternatively, for a simpler approach, you could try to read a "sample.html"
        //    file next to the component definition if it exists.
        // 4. For this example, we'll keep a simplified structure.

        // Consider using a more descriptive class name based on the component's name
        String componentClassName = CMP_CLASS_PREFIX + resource.getName().toLowerCase().replace(" ", "-");
        return String.format(
                "<div class=\"%s\">\n  \n</div>",
                componentClassName,
                getComponentName(resource)
        );
    }
}
