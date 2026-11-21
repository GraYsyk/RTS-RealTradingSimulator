package net.graysenko.RTS.MarketMaker.Records;

import java.math.BigDecimal;

public record MarketMakerSettings(int levels, BigDecimal gridStepPercent, BigDecimal zonePercent, BigDecimal quantity) {
    public MarketMakerSettings {
        if(levels <= 0) throw new IllegalArgumentException("levels cannot be negative");
        if(gridStepPercent == null || gridStepPercent.signum() <= 0) throw new IllegalArgumentException("grid step percent cannot be negative");
        if(zonePercent == null || zonePercent.signum() <= 0) throw new IllegalArgumentException("zone percent cannot be negative");
        if(quantity == null || quantity.signum() <= 0) throw new IllegalArgumentException("quantity cannot be negative");
    }
}
