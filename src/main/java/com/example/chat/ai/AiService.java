package com.example.chat.ai;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.apache.http.HttpException;

import java.io.IOException;

public final class AiService {
    private static final String API_KEY;
    private static final Client client;

    static {
        String key = System.getenv("GEMINI_API_KEY");
        if (key == null || key.isEmpty()) {
            throw new IllegalStateException("Environment variable GEMINI_API_KEY must be set");
        }
        API_KEY = key;
        client = Client.builder()
                .apiKey(API_KEY)
                .build();
    }

    private AiService() {}

    public static String ask(String prompt) throws HttpException, IOException {
        GenerateContentResponse res =
                client.models.generateContent("gemini-2.0-flash-001", prompt, null);
        return res.text();
    }
}