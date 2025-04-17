package com.mysite.core.AemMigration.configs.impl;

import com.mysite.core.AemMigration.configs.ComponentResourceTypeConfig;
import com.mysite.core.AemMigration.configs.ComponentResourceTypeService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
@Component(service = ComponentResourceTypeService.class,immediate = true)
@Designate(ocd= ComponentResourceTypeConfig.class)
public class ComponentResourceTypeServiceImpl implements ComponentResourceTypeService{
    public Set<String> multifieldTypes;
    public Set<String> tabTypes;
    public Set<String> containerTypes;
    public Set<String> fixedColumnsTypes;
    public Set<String> wellTypes;

    @Activate
    @Modified
    private void activate(ComponentResourceTypeConfig config)
    {
        multifieldTypes= new HashSet<>(Arrays.asList(config.multifieldResourceTypes()));
        tabTypes=new HashSet<>(Arrays.asList(config.tabResourceTypes()));
        containerTypes=new HashSet<>(Arrays.asList(config.containerResourceTypes()));
        fixedColumnsTypes=new HashSet<>(Arrays.asList(config.fixedColumnsResourceTypes()));
        wellTypes=new HashSet<>(Arrays.asList(config.wellResourceTypes()));
    }

    @Override
    public Set<String> getMultiFieldTypes() {
        return multifieldTypes;
    }

    @Override
    public Set<String> getTabTypes() {
        return tabTypes;
    }

    @Override
    public Set<String> getContainerTypes() {
        return containerTypes;
    }

    @Override
    public Set<String> getFixedColumnTypes() {
        return fixedColumnsTypes;
    }

    @Override
    public Set<String> getWellTypes() {
        return wellTypes;
    }
}
