package com.mysite.core.AemMigration.service;

import com.google.gson.JsonObject;
import org.apache.sling.api.resource.Resource;

public interface ContentIteratorService {
    JsonObject getContent(Resource resource, String pagePath);
}
