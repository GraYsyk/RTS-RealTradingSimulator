package net.graysenko.RTS.MatchingEngine.Engine;

import net.graysenko.RTS.MatchingEngine.Enums.Side;
import net.graysenko.RTS.MatchingEngine.POJO.Order;
import net.graysenko.RTS.MatchingEngine.POJO.PriceLevel;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class OrderBook {
    private final TreeMap<BigDecimal, PriceLevel> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<BigDecimal, PriceLevel> asks = new TreeMap<>();

    public void addOrder(Order order) {
        if (order == null) return;

        TreeMap<BigDecimal, PriceLevel> book = order.getSide() == Side.BUY ? bids : asks;
        PriceLevel level = book.computeIfAbsent(order.getPrice(), k -> new PriceLevel());
        level.addOrder(order);
    }

    public void removeLevelIfEmpty(BigDecimal price, Side side) {
        TreeMap<BigDecimal, PriceLevel> book = (side == Side.BUY) ? bids : asks;
        PriceLevel level = book.get(price);
        if (level != null && level.isEmpty()) book.remove(price);
    }

    public Map.Entry<BigDecimal, PriceLevel> bestBid() {
        return bids.firstEntry();
    }

    public Map.Entry<BigDecimal, PriceLevel> bestAsk() {
        return asks.firstEntry();
    }
}
