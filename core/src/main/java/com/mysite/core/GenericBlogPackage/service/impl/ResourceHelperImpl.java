package com.mysite.core.GenericBlogPackage.service.impl;

import java.util.HashMap;
import java.util.Map;

import com.mysite.core.GenericBlogPackage.constants.UserManagementConstants;
import com.mysite.core.GenericBlogPackage.service.ResourceHelper;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@Component(service = ResourceHelper.class, immediate = true)
public class ResourceHelperImpl implements ResourceHelper {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	@Reference
	ResourceResolverFactory resResolverFactory;

	@Override
	public ResourceResolver getResourceResolver() throws LoginException {
		try {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put(ResourceResolverFactory.SUBSERVICE, UserManagementConstants.SUB_SERVICE);
			ResourceResolver resourceResolver = resResolverFactory.getServiceResourceResolver(map);
			logger.error("resourceResolver" + resourceResolver);
			return resourceResolver;
		} catch (Exception e) {
			logger.error("error on resource" + e.getMessage());
		}
		return null;
	}

}
