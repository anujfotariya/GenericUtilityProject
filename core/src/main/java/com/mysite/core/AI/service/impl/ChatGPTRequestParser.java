package com.mysite.core.AI.service.impl;

import com.mysite.core.AI.service.IChatGPTRequest;
import com.mysite.core.AI.service.RequestParser;
import org.osgi.service.component.annotations.Component;

@Component(service = RequestParser.class, immediate = true)
public class ChatGPTRequestParser implements RequestParser {


    @Override
    public IChatGPTRequest parse(String requestPrompt, String format, String role) {
        switch (format) {
            case RequestParser.Chat:
                return createRequest(requestPrompt, role);

            case RequestParser.Image:
                return createRequestForImageGeneration(requestPrompt);
        }
        return null;

    }

    private IChatGPTRequest createRequestForImageGeneration(String requestPrompt) {
        return null;

    }

    private IChatGPTRequest createRequest(String requestPrompt, String role) {
        return ChatGPTRequest.getChatGPTRequest(getModel(), getTemperature(), getToken(), requestPrompt, getModel(),
                role);

    }

    private String getModel() {
        return "gpt-3.5-turbo";
    }

    private double getTemperature() {
        return 1.0;
    }

    private int getToken() {
        return 2000;
    }

    private String formatType() {
        return "html";
    }

}
