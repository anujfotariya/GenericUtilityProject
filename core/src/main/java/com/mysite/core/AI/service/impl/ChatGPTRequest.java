package com.mysite.core.AI.service.impl;

import com.mysite.core.AI.service.IChatGPTRequest;

public class ChatGPTRequest implements IChatGPTRequest {

    private static String model;
    private double temperature;
    private int max_tokens;
    private String prompt;
    private String formatType;
    private String role;

    public String getModel() {
        return model;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getMax_tokens() {
        return max_tokens;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getFormatType() {
        return "html";
    }

    public String getRole() {
        return role;
    }

    public static ChatGPTRequest getChatGPTRequest(String model, double temperature, int max_tokens, String prompt,
                                                   String formatType, String role) {

        ChatGPTRequest chatGPTRequest = new ChatGPTRequest();

        chatGPTRequest.model = model;
        chatGPTRequest.temperature = temperature;
        chatGPTRequest.max_tokens = max_tokens;
        chatGPTRequest.prompt = prompt;
        chatGPTRequest.formatType = formatType;
        chatGPTRequest.role = role;

        return chatGPTRequest;
    }

}
