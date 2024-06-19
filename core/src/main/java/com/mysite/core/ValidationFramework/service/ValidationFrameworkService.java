package com.mysite.core.ValidationFramework.service;

import java.util.List;
import java.util.Map;

public interface ValidationFrameworkService {

    List<Map<String,String>> getSingleFieldValues();

    List<Map<String,String>> getMultiFieldValues();
}
