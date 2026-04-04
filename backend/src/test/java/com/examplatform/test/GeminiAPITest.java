package com.examplatform.test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GeminiAPITest {
    public static void main(String[] args) {
        try {
            String apiKey = "AIzaSyCTxmK-fp9cG9Y6nfWuY3Ihk8x1xDsiHVM";
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";
            
            String requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"Generate 2 multiple choice questions about Java Programming. Return ONLY a valid JSON array with this structure: [{\\\"question\\\":\\\"What is Java?\\\",\\\"options\\\":[\\\"Option A: Programming language\\\",\\\"Option B: Coffee\\\",\\\"Option C: Island\\\",\\\"Option D: Framework\\\"],\\\"correctAnswer\\\":\\\"Option A: Programming language\\\"}]\"}]}]}";
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "?key=" + apiKey))
             .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
            
            System.out.println("Testing Gemini API...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("Status Code: " + response.statusCode());
            System.out.println("Response Body: " + response.body());
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
