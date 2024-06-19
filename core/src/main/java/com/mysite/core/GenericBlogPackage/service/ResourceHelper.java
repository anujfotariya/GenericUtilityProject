package com.mysite.core.GenericBlogPackage.service;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;

public interface ResourceHelper {
    public ResourceResolver getResourceResolver() throws LoginException;
}
