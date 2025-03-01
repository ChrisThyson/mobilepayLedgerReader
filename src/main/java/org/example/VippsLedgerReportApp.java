package org.example;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Scanner;

public class VippsLedgerReportApp {
    private static final String BASE_URL = "https://api.vipps.no";

    private final VippsCredentialManager credentialManager;
    private final VippsAuthService authService;
    private final VippsHttpClient httpClient;
    private final VippsLedgerReportService reportService;

    public VippsLedgerReportApp() {
        this.credentialManager = new VippsCredentialManager();
        this.httpClient = new VippsHttpClient();
        this.authService = new VippsAuthService(credentialManager, BASE_URL);
        this.reportService = new VippsLedgerReportService(authService, httpClient, credentialManager, BASE_URL);
    }

    public static void main(String[] args) {
        VippsLedgerReportApp app = new VippsLedgerReportApp();
        try {
            app.run();
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs the application workflow
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    public void run() throws IOException, InterruptedException {
        System.out.println("Vipps MobilePay Ledger Report Application");
        System.out.println("----------------------------------------");

        // Step 1: Authenticate
        System.out.println("Authenticating with Vipps MobilePay API...");
        if (!authService.authenticate()) {
            System.err.println("Authentication failed. Please check your credentials.");
            return;
        }
        System.out.println("Authentication successful!");

        // Step 2: Get date range from user or use default (last 30 days)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        Scanner scanner = new Scanner(System.in);
        System.out.println("\nDefault date range: " + startDate + " to " + endDate);
        System.out.print("Use default date range? (y/n): ");
        String useDefault = scanner.nextLine().trim().toLowerCase();

        if (!useDefault.equals("y") && !useDefault.equals("yes")) {
            System.out.print("Enter start date (YYYY-MM-DD): ");
            String startDateStr = scanner.nextLine().trim();
            System.out.print("Enter end date (YYYY-MM-DD): ");
            String endDateStr = scanner.nextLine().trim();

            try {
                startDate = LocalDate.parse(startDateStr);
                endDate = LocalDate.parse(endDateStr);
            } catch (Exception e) {
                System.out.println("Invalid date format. Using default date range.");
            }
        }

        // Step 3: Request a ledger report
        System.out.println("\nRequesting ledger report for period: " + startDate + " to " + endDate);
        String reportId = reportService.requestLedgerReport(startDate, endDate);

        if (reportId == null) {
            System.err.println("Failed to request ledger report.");
            return;
        }

        System.out.println("Ledger report requested successfully, ID: " + reportId);

        // Step 4: Retrieve the report
        System.out.println("\nRetrieving ledger report...");
        String reportPath = reportService.retrieveLedgerReport(reportId);

        if (reportPath != null) {
            System.out.println("\nReport successfully retrieved and saved to: " + reportPath);
        } else {
            System.err.println("Failed to retrieve ledger report.");
        }
    }
}