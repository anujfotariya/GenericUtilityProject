package com.mysite.core.AemMigration.service;

import org.apache.sling.api.resource.Resource;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Map;

public interface ComponentListService {
    Map<String, List<String>> scanComponentsRecursive(Resource rootResource, List<String> multifieldList, List<String> componentsWithMultifield,String caller) throws RepositoryException;
}
