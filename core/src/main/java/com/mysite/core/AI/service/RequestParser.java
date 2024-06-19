package com.mysite.core.AI.service;

public interface RequestParser {
    public static String Chat = "chat-completion";
    public static String Image = "images-generations";

    IChatGPTRequest parse(String requestPrompt, String format, String role);
}
