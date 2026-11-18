package net.graysenko.RTS.MarketMaker;

import lombok.extern.slf4j.Slf4j;
import net.graysenko.RTS.MarketMaker.Records.PriceResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Component
@Slf4j
public class BinancePriceProvider implements PriceProvider {

    private final RestClient restClient;

    public BinancePriceProvider(RestClient.Builder builder) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(3));

        this.restClient = builder
                .baseUrl("https://api.binance.com")
                .requestFactory(factory)
                .build();
    }

    @Override
    public Optional<BigDecimal> getPriceForAsset(String symbol) {
        try {
            PriceResponse r = restClient.get()
                    .uri(b -> b.path("/api/v3/ticker/price")
                            .queryParam("symbol", symbol + "USDT")
                            .build())
                    .retrieve()
                    .body(PriceResponse.class);
            if (r != null && r.price() != null) {
                BigDecimal price = new BigDecimal(r.price());
                log.info("Got price {} for {}", price, symbol);
                return Optional.of(price);
            }
            log.warn("Failed to fetch price for {}, price or response is null", symbol);
            return Optional.empty();
        } catch (RestClientException | NumberFormatException e) {
            log.warn("Failed to fetch price for {}", symbol, e);
            return Optional.empty();
        }
    }
}
