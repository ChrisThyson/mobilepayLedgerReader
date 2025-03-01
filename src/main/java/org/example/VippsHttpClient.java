package org.example;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

public class VippsHttpClient {
    private final HttpClient client;

    public VippsHttpClient() {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Sends an HTTP request and returns the response
     * @param request the HTTP request to send
     * @return the HTTP response
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    public HttpResponse<String> sendRequest(HttpRequest request) throws IOException, InterruptedException {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Creates an authentication request
     * @param credentials the credentials manager containing API keys
     * @param baseUrl the base URL for the API
     * @return the HTTP request
     */
    public HttpRequest createAuthRequest(VippsCredentialManager credentials, String baseUrl) {
        String auth = credentials.getClientId() + ":" + credentials.getClientSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/accessToken/get"))
                .header("client_id", credentials.getClientId())
                .header("client_secret", credentials.getClientSecret())
                .header("Ocp-Apim-Subscription-Key", credentials.getSubscriptionKey())
                .header("Authorization", "Basic " + encodedAuth)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
    }

    /**
     * Creates a request to create a new ledger report
     * @param token the access token
     * @param subscriptionKey the subscription key
     * @param baseUrl the base URL for the API
     * @param body the JSON body for the request
     * @return the HTTP request
     */
    public HttpRequest createReportRequest(String token, String subscriptionKey, String baseUrl, JSONObject body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/vipps-report/v1/report"))
                .header("Authorization", "Bearer " + token)
                .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
    }

    /**
     * Creates a request to check the status of a report
     * @param token the access token
     * @param subscriptionKey the subscription key
     * @param baseUrl the base URL for the API
     * @param reportId the ID of the report to check
     * @return the HTTP request
     */
    public HttpRequest createStatusRequest(String token, String subscriptionKey, String baseUrl, String reportId) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/vipps-report/v1/report/" + reportId))
                .header("Authorization", "Bearer " + token)
                .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                .GET()
                .build();
    }


    /**
     * Creates a request to download a report
     * @param reportUrl the URL of the report to download
     * @return the HTTP request
     */
    public HttpRequest createDownloadRequest(String reportUrl) {
        return HttpRequest.newBuilder()
                .uri(URI.create(reportUrl))
                .GET()
                .build();
    }
}
