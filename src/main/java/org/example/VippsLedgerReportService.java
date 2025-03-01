package org.example;

import org.json.JSONObject;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class VippsLedgerReportService {
    private final String baseUrl;
    private final VippsAuthService authService;
    private final VippsHttpClient httpClient;
    private final VippsCredentialManager credentialManager;

    public VippsLedgerReportService(VippsAuthService authService, VippsHttpClient httpClient,
                                    VippsCredentialManager credentialManager, String baseUrl) {
        this.authService = authService;
        this.httpClient = httpClient;
        this.credentialManager = credentialManager;
        this.baseUrl = baseUrl;
    }

    /**
     * Requests a new ledger report for the specified date range
     * @param startDate the start date for the report
     * @param endDate the end date for the report
     * @return the report ID if successful, null otherwise
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    public String requestLedgerReport(LocalDate startDate, LocalDate endDate) throws IOException, InterruptedException {
        // Get access token
        String accessToken = authService.getAccessToken();

        // Format dates
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Create request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("format", "CSV");
        requestBody.put("startDate", startDate.format(formatter));
        requestBody.put("endDate", endDate.format(formatter));

        // Create and send request
        var request = httpClient.createReportRequest(
                accessToken,
                credentialManager.getSubscriptionKey(),
                baseUrl,
                requestBody
        );

        HttpResponse<String> response = httpClient.sendRequest(request);

        if(response.statusCode() != 200) {
            System.err.println("Request failed with status code " + response.statusCode() +
                    ", Response: " + response.body());
            return null;
        }

        // Parse the response to get the report ID
        JSONObject jsonResponse = new JSONObject(response.body());
        return jsonResponse.getString("reportId");
    }

    /**
     * Polls for the status of a report until it's ready or max attempts reached
     * @param reportId the ID of the report to check
     * @return the report URL if ready, null otherwise
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    public String pollReportStatus(String reportId) throws IOException, InterruptedException {
        boolean reportReady = false;
        int maxAttempts = 10;
        int attempt = 0;
        String reportUrl = null;

        System.out.println("Waiting for report to be generated...");

        // Poll until the report is ready or max attempts reached
        while (!reportReady && attempt < maxAttempts) {
            attempt++;

            // Get access token (will refresh if needed)
            String accessToken = authService.getAccessToken();

            // Build and send request
            var statusRequest = httpClient.createStatusRequest(
                    accessToken,
                    credentialManager.getSubscriptionKey(),
                    baseUrl,
                    reportId
            );

            HttpResponse<String> statusResponse = httpClient.sendRequest(statusRequest);

            if (statusResponse.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(statusResponse.body());
                String status = jsonResponse.getString("status");

                if ("COMPLETED".equals(status)) {
                    reportReady = true;
                    reportUrl = jsonResponse.getString("reportUrl");
                    System.out.println("Report is ready!");
                } else {
                    System.out.println("Report status: " + status + ", waiting 5 seconds before checking again...");
                    Thread.sleep(5000); // Wait 5 seconds before checking again
                }
            } else {
                System.out.println("Failed to check report status, attempt " + attempt + " of " + maxAttempts +
                        ". Status code: " + statusResponse.statusCode());
                Thread.sleep(5000);
            }
        }

        if (!reportReady) {
            System.err.println("Report not ready after " + maxAttempts + " attempts");
            return null;
        }

        return reportUrl;
    }

    /**
     * Downloads a report from the given URL and saves it to the file system
     * @param reportUrl the URL of the report to download
     * @param reportId the ID of the report (used for filename)
     * @return the path to the downloaded file
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    public String downloadReport(String reportUrl, String reportId) throws IOException, InterruptedException {
        // Create and send the download request
        var downloadRequest = httpClient.createDownloadRequest(reportUrl);
        HttpResponse<String> downloadResponse = httpClient.sendRequest(downloadRequest);

        if (downloadResponse.statusCode() == 200) {
            // Save the report to a file
            String reportContent = downloadResponse.body();
            String fileName = "vipps_ledger_report_" + reportId + ".csv";
            Path filePath = Paths.get(fileName);

            Files.writeString(filePath, reportContent);
            System.out.println("Report successfully downloaded and saved to: " + fileName);
            return filePath.toAbsolutePath().toString();
        } else {
            System.err.println("Failed to download report: " + downloadResponse.statusCode());
            return null;
        }
    }

    /**
     * Retrieves a ledger report, polling for status and downloading when ready
     * @param reportId the ID of the report to retrieve
     * @return the path to the downloaded file, or null if retrieval failed
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    public String retrieveLedgerReport(String reportId) throws IOException, InterruptedException {
        String reportUrl = pollReportStatus(reportId);

        if (reportUrl != null) {
            return downloadReport(reportUrl, reportId);
        }

        return null;
    }
}
