package com.mysite.core.AemMigration.configs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Component Dialog Resource Types Config")
public @interface ComponentResourceTypeConfig {

    @AttributeDefinition(name = "Multifield Resource Type")
    String[] multifieldResourceTypes() default {
            "granite/ui/components/coral/foundation/form/multifield"
    };

    @AttributeDefinition(name = "Tab Resource Types")
    String[] tabResourceTypes() default {
            "granite/ui/components/coral/foundation/tabs"
    };

    @AttributeDefinition(name = "Container Resource Types")
    String[] containerResourceTypes() default {
            "granite/ui/components/coral/foundation/container"
    };

    @AttributeDefinition(name = "Fixed Columns Resource Types")
    String[] fixedColumnsResourceTypes() default {
            "granite/ui/components/coral/foundation/fixedcolumns"
    };

    @AttributeDefinition(name = "Well Resource Types")
    String[] wellResourceTypes() default {
            "granite/ui/components/coral/foundation/well"
    };
}
