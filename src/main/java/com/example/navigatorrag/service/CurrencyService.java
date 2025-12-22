package com.example.navigatorrag.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CurrencyService {
    private final RestClient restClient;

    @Value("${currency.api.key}")
    private String apiKey;

    @Value("${currency.api.base-url}")
    private String baseUrl;

    public CurrencyService(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public double convertFromRsd(double amountRsd, String targetCurrency) {
        System.out.println("\n[SERVICE] CurrencyService call initiated.");
        System.out.println("[SERVICE] Parameters: Amount=" + amountRsd + ", Target=" + targetCurrency);
        try {
            JsonNode response = restClient.get()
                    .uri(baseUrl + "?apikey={key}", apiKey)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("rates")) {
                JsonNode rates = response.get("rates");
                double rsdToUsdRate = rates.get("RSD").asDouble();
                double targetToUsdRate = rates.get(targetCurrency.toUpperCase()).asDouble();

                System.out.println("[API SUCCESS] Retrieved rates from CurrencyFreaks.");
                return (amountRsd / rsdToUsdRate) * targetToUsdRate;
            }
        } catch (Exception e) {
            System.out.println("CurrencyFreaks API Error: " + e.getMessage());
        }
        return 0.0;
    }
}
