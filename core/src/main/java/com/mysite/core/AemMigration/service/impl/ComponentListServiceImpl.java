package com.mysite.core.AemMigration.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mysite.core.AemMigration.configs.ComponentResourceTypeService;
import com.mysite.core.AemMigration.service.ComponentListService;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.*;

@Component(service = ComponentListService.class,immediate = true)
public class ComponentListServiceImpl implements ComponentListService{
    Logger logger= LoggerFactory.getLogger(this.getClass());

    @Reference
    ComponentResourceTypeService componentResourceTypeService;


    Set<String> multiFieldType;
    Set<String> tabTypes;
    Set<String> containerTypes;

    Set<String>fixedColumnTypes;

    Set<String>wellTypes;

    @Activate
    protected void activate(){
        multiFieldType= componentResourceTypeService.getMultiFieldTypes();
        tabTypes=componentResourceTypeService.getTabTypes();
        containerTypes=componentResourceTypeService.getContainerTypes();
        fixedColumnTypes=componentResourceTypeService.getFixedColumnTypes();
        wellTypes=componentResourceTypeService.getWellTypes();
    }


    @Override
    public Map<String, List<String>> scanComponentsRecursive(Resource resource, List<String> multifieldList, List<String> componentsWithMultifield, String caller) throws RepositoryException {
        if (isComponent(resource)) {
            Resource dialogResource = resource.getChild("cq:dialog");
            processDialogElement(dialogResource, multifieldList, componentsWithMultifield);
        }

        if ("Servlet".equals(caller)) {
            for (Resource child : resource.getChildren()) {
                scanComponentsRecursive(child, multifieldList, componentsWithMultifield, caller);
            }
        }

        Map<String, List<String>> result = new HashMap<>();
        result.put("Multifield", multifieldList);
        result.put("NestedMultifield", componentsWithMultifield);
        return result;
    }

    private boolean isComponent(Resource resource) {
        Node node = resource.adaptTo(Node.class);
        try {
            return node != null && "cq:Component".equals(node.getProperty("jcr:primaryType").getString()) && resource.getChild("cq:dialog") != null;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private void processDialogElement(Resource resource, List<String> multifieldList, List<String> componentsWithMultifield) throws RepositoryException {
        if (resource == null) return;
        String resourceType = getPropertyValue(resource, "sling:resourceType");

        if ("cq/gui/components/authoring/dialog".equals(resourceType)) {
            Resource items = Optional.ofNullable(resource.getChild("content/items/column/items")).orElse(resource.getChild("content/items"));
            if (items != null) {
                processChildren(items, multifieldList, componentsWithMultifield);
            }
        } else if (tabTypes.contains(resourceType)) {
            Optional.ofNullable(resource.getChild("items"))
                    .ifPresent(items -> items.getChildren()
                            .forEach(tab -> Optional.ofNullable(tab.getChild("items"))
                                    .ifPresent(tabItems -> {
                                        try {
                                            processChildren(tabItems, multifieldList, componentsWithMultifield);
                                        } catch (RepositoryException e) {
                                            logger.error("Error in tab processing", e);
                                        }
                                    })));
        } else if (multiFieldType.contains(resourceType)) {
            String basePath = resource.getPath().split("/cq:dialog")[0];
            multifieldList.add(basePath);

            if (containsNestedMultifield(resource)) {
                componentsWithMultifield.add(basePath);
            }
        }
    }

    private void processChildren(Resource resource, List<String> multifieldList, List<String> componentsWithMultifield) throws RepositoryException {
        for (Resource child : resource.getChildren()) {
            String resourceType = getPropertyValue(child, "sling:resourceType");
            if (containerTypes.contains(resourceType)
                    || fixedColumnTypes.contains(resourceType)
                    || wellTypes.contains(resourceType)) {
                Optional.ofNullable(child.getChild("items"))
                        .ifPresent(items -> {
                            try {
                                processChildren(items, multifieldList, componentsWithMultifield);
                            } catch (RepositoryException e) {
                                logger.error("Error processing child items", e);
                            }
                        });
            } else if (tabTypes.contains(resourceType) || (resourceType != null && resourceType.contains("/foundation/form/"))) {
                processDialogElement(child, multifieldList, componentsWithMultifield);
            }
        }
    }

    private boolean containsNestedMultifield(Resource resource) throws RepositoryException {
        Resource fieldResource = resource.getChild("field/items");
        if (fieldResource != null) {
            List<String> nestedPaths = new ArrayList<>();
            processChildrenForNestedField(fieldResource, nestedPaths);
            return !nestedPaths.isEmpty();
        }
        return false;
    }

    private void processChildrenForNestedField(Resource resource, List<String> pathList) throws RepositoryException {
        for (Resource child : resource.getChildren()) {
            String resourceType = getPropertyValue(child, "sling:resourceType");
            if (containerTypes.contains(resourceType) || fixedColumnTypes.contains(resourceType) || wellTypes.contains(resourceType)) {
                Optional.ofNullable(child.getChild("items"))
                        .ifPresent(items -> {
                            try {
                                processChildrenForNestedField(items, pathList);
                            } catch (RepositoryException e) {
                                logger.error("Error in nested field", e);
                            }
                        });
            } else if (tabTypes.contains(resourceType) || (resourceType != null && resourceType.contains("/foundation/form/"))) {
                processDialogElementForNestedField(child, pathList);
            }
        }
    }

    private void processDialogElementForNestedField(Resource resource, List<String> pathList) throws RepositoryException {
        String resourceType = getPropertyValue(resource, "sling:resourceType");
        if (tabTypes.contains(resourceType)) {
            Optional.ofNullable(resource.getChild("items"))
                    .ifPresent(items -> items.getChildren().forEach(tab -> Optional.ofNullable(tab.getChild("items"))
                            .ifPresent(tabItems -> {
                                try {
                                    processChildrenForNestedField(tabItems, pathList);
                                } catch (RepositoryException e) {
                                    logger.error("Error in nested tab", e);
                                }
                            })));
        } else if (multiFieldType.contains(resourceType)) {
            pathList.add(resource.getPath());
        }
    }

    private String getPropertyValue(Resource resource, String propertyName) {
        return resource.getValueMap().getOrDefault(propertyName, "").toString();
    }
}
