package com.mysite.core.GenericBlogPackage.service;

import java.util.List;
import java.util.Map;

public interface GenericBlogService {

    public List<Map<String,Object>> getBlogs(String pagePath,String limit,List<String> tags);
}
