package net.graysenko.RTS.MarketMaker.Records;

import net.graysenko.RTS.MatchingEngine.Enums.Side;

import java.math.BigDecimal;

public record BotOrderSpec(Side side, BigDecimal price, BigDecimal quantity) {
    public BotOrderSpec {
        if (side == null) throw new IllegalArgumentException("side cannot be null");
        if (price == null || price.signum() <= 0) throw new IllegalArgumentException("price cannot be null");
        if (quantity == null || quantity.signum() <= 0) throw new IllegalArgumentException("quantity cannot be null");

        price = price.setScale(2); //ensures, that strategy price is gonna be correctly rounded
    }
}
