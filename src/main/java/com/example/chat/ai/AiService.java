package com.example.chat.ai;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.apache.http.HttpException;

import java.io.IOException;

public final class AiService {

    private static final String API_KEY =
            System.getenv("GOOGLE_API_KEY") != null
                    ? System.getenv("GOOGLE_API_KEY")
                    : "AIzaSyDzWJuDotseT1Cyg5Frl8guXgF8F_ib-cw";

    private static final Client client = Client.builder()
            .apiKey(API_KEY)
            .build();

    private AiService() {}

    public static String ask(String prompt) throws HttpException, IOException {
        GenerateContentResponse res =
                client.models.generateContent("gemini-2.0-flash-001", prompt, null);
        return res.text();
    }
}
