package com.mysite.core.AemMigration.configs;

import java.util.Set;

public interface ComponentResourceTypeService {

    public Set<String> getMultiFieldTypes();

    public Set<String> getTabTypes();

    public Set<String> getContainerTypes();

    public Set<String> getFixedColumnTypes();

    public Set<String> getWellTypes();
}
