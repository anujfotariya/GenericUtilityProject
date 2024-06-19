package com.mysite.core.AI.service.config.impl;

import com.mysite.core.AI.service.config.GenericConfigService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.Collections;
import java.util.Map;

@Designate(ocd = GenericConfigServiceImpl.Config.class)
@Component(service = GenericConfigService.class, immediate = true)
public class GenericConfigServiceImpl implements GenericConfigService {
    private Map<String, String> configMap;

    @ObjectClassDefinition(name = "Generic - Configuration")
    public @interface Config {

        @AttributeDefinition(name = "ChatGptApiKey",required = false,type = AttributeType.STRING)
        String setChatGptKey() default "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";

        @AttributeDefinition(name = "ChatGptApiImageKey",required = false,type = AttributeType.STRING)
        String setChatGptImageKey() default "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";

        @AttributeDefinition(name = "ChatGptApiEndpoint",required = false,type = AttributeType.STRING)
        String setChatgptApiEndpoint() default "";

        @AttributeDefinition(name = "ChatGptApiImageEndpoint",required = false,type = AttributeType.STRING)
        String setChatgptApiImageEndpoint() default "";

    }

    @Activate
    @Modified
    protected void activate(Config config) {
        this.configMap.put("set-ChatGpt-Key",config.setChatGptKey());
        this.configMap.put("set-chatGptImage-key",config.setChatGptImageKey());
        this.configMap.put("set-chatGptApiEndpoint",config.setChatgptApiEndpoint());
        this.configMap.put("set-chatGptApiImageEndpoint",config.setChatgptApiImageEndpoint());

    }
    @Override
    public Map<String, String> getConfig() {
        return Collections.unmodifiableMap(this.configMap);
    }

}
