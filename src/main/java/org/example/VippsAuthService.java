package org.example;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

public class VippsAuthService {
    private final HttpClient client;
    private final String baseUrl;
    private final VippsCredentialManager credentialManager;
    private String accessToken;
    private Instant tokenExpiration;

    public VippsAuthService(VippsCredentialManager credentialManager, String baseUrl) {
        this.credentialManager = credentialManager;
        this.baseUrl = baseUrl;
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }


    /**
     * Authenticates with the Vipps API to get an access token
     * @return true if authentication was successful
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    public boolean authenticate() throws IOException, InterruptedException {
        // Create Basic Auth header
        String auth = credentialManager.getClientId() + ":" + credentialManager.getClientSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        // Build the request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/accessToken/get"))
                .header("client_id", credentialManager.getClientId())
                .header("client_secret", credentialManager.getClientSecret())
                .header("Ocp-Apim-Subscription-Key", credentialManager.getSubscriptionKey())
                .header("Authorization", "Basic " + encodedAuth)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        // Send the request
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() != 200) {
            System.err.println("Authentication failed with status code " + response.statusCode() +
                    ", Response: " + response.body());
            return false;
        }

        // Parse the response to get the access token and expiration time
        JSONObject jsonResponse = new JSONObject(response.body());
        accessToken = jsonResponse.getString("access_token");

        // Calculate token expiration time (typically 1 hour from now, but we can get from response)
        int expiresIn = jsonResponse.getInt("expires_in");
        tokenExpiration = Instant.now().plusSeconds(expiresIn);

        return true;
    }

    /**
     * Gets a valid access token, authenticating if necessary
     * @return the access token
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    public String getAccessToken() throws IOException, InterruptedException {
        if(!isTokenValid()) {
            authenticate();
        }
        return accessToken;
    }

    /**
     * Checks if the current token is valid
     * @return true if the token is valid and not expired
     */
    public boolean isTokenValid() {
        // Token is valid if it exists and has at least 5 minutes before expiration
        return accessToken != null &&
                tokenExpiration != null &&
                Instant.now().plusSeconds(300).isBefore(tokenExpiration);
    }
}
