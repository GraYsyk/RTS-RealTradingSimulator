package net.graysenko.RTS.MarketMaker;

import java.math.BigDecimal;
import java.util.Optional;

public interface PriceProvider {
    Optional<BigDecimal> getPriceForAsset(String symbol);
}
