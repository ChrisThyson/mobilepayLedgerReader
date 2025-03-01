package org.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;

public class VippsCredentialManager {
    private String clientId;
    private String clientSecret;
    private String subscriptionKey;
    private static final String CONFIG_FILE = "vipps_credentials.properties";

    public VippsCredentialManager() {
        // Try to load credentials from file first
        if(!loadFromFile()) {
            // If no file exists or loading fails, collect from user input
            collectCredentials();
            // Save the collected credentials for future use
            saveToFile();
        }
    }

    /**
     * Collects Vipps MobilePay API credentials from user input
     */
    public void collectCredentials() {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter your Client ID: ");
        clientId = scanner.nextLine();

        System.out.print("Enter your Client Secret: ");
        clientSecret = scanner.nextLine();

        System.out.print("Enter your Subscription Key: ");
        subscriptionKey = scanner.nextLine();
    }

    /**
     * Loads credentials from properties file if available
     * @return
     */
    public boolean loadFromFile() {
        File configFile = new File(CONFIG_FILE);
        if(!configFile.exists()) {
            return false;
        }

        Properties properties = new Properties();
        try(FileInputStream input = new FileInputStream(configFile)) {
            properties.load(input);

            clientId = properties.getProperty("client.id");
            clientSecret = properties.getProperty("client.secret");
            subscriptionKey = properties.getProperty("subscription.key");

            // Check if all credentials are present
            return clientId != null && !clientId.isEmpty() &&
                    clientSecret != null && !clientSecret.isEmpty() &&
                    subscriptionKey != null && !subscriptionKey.isEmpty();
        } catch (IOException e) {
            System.err.println("Error loading credentials: " + e.getMessage());
            return false;
        }
    }

    /**
     * Savs crednetials to properties file for future use
     */
    public void saveToFile() {
        Properties properties = new Properties();
        properties.setProperty("client.id", clientId);
        properties.setProperty("client.secret", clientSecret);
        properties.setProperty("subscription.key", subscriptionKey);

        try(FileOutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "Vipps MobilePay API Credentials");
            System.out.println("Credentials saved to " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("Error saving credentials: " + e.getMessage());
        }
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getSubscriptionKey() {
        return subscriptionKey;
    }
}
