package com.example.navigatorrag.config;

import com.example.navigatorrag.dto.CurrencyRequest;
import com.example.navigatorrag.dto.SalesRequest;
import com.example.navigatorrag.service.CurrencyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
public class ToolConfig {

    private final CurrencyService currencyService;

    public ToolConfig(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @Bean
    @Description("Dobavlja iznos prodaje za klijenta i mesec.")
    public Function<SalesRequest, String> getSalesData() {
        return request -> {
            System.out.println("\n[TOOL CALL] AI is executing 'getSalesData'...");
            System.out.println("[TOOL CALL] Parameters: Client=" + request.clientName() + ", Month=" + request.month());

            if(request.clientName().equalsIgnoreCase("Petar Petrovic")) {
                return "Prodaja za klijenta " + request.clientName() + " u mesecu" + request.month() + "iznosi 12.000 RSD";
            }
            return "Nema podataka o prodaji za klijenta: " + request.clientName();
        };
    }

    @Bean
    @Description("Konvertuje iznos iz RSD u bilo koju drugu svetsku valutu koristeći live kurs.")
    public Function<CurrencyRequest, String> convertCurrency() {
        return request -> {
            double result = currencyService.convertFromRsd(request.amountRsd(), request.targetCurrency());

            if (result > 0) {
                return String.format("REZULTAT KONVERZIJE: %.2f RSD je %.2f %s",
                        request.amountRsd(), result, request.targetCurrency().toUpperCase());
            }
            return "Greska: Valuta " + request.targetCurrency() + " nije podrzana.";
        };
    }
}
