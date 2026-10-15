package net.graysenko.RTS;

import net.graysenko.RTS.MatchingEngine.Engine.MatchingEngine;
import net.graysenko.RTS.MatchingEngine.Engine.OrderBook;
import net.graysenko.RTS.MatchingEngine.Enums.OrderType;
import net.graysenko.RTS.MatchingEngine.Enums.Side;
import net.graysenko.RTS.MatchingEngine.Enums.Status;
import net.graysenko.RTS.MatchingEngine.POJO.Order;
import net.graysenko.RTS.MatchingEngine.Records.Trade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class METests {

    private Order buy(long id, String price, String quantity) {
        return new Order(id, 1L, Side.BUY, OrderType.LIMIT,
                new BigDecimal(price), new BigDecimal(quantity), Instant.now());
    }

    private Order marketBuy(long id, String qty) {
        return new Order(id, 1L, Side.BUY, OrderType.MARKET,
                null, new BigDecimal(qty), Instant.now());   // price = null
    }

    private Order sell(long id, String price, String quantity) {
        return new Order(id, 2L, Side.SELL, OrderType.LIMIT,
                new BigDecimal(price), new BigDecimal(quantity), Instant.now());
    }

    @Test
    void addedOrderBecomesBestBid() {
        OrderBook book = new OrderBook();
        book.addOrder(buy(1, "100", "1"));

        assertNotNull(book.bestBid());
        assertEquals(0, book.bestBid().getKey().compareTo(new BigDecimal("100")));
    }

    @Test
    void buyWithNoAsksRestInBook() {
        OrderBook book = new OrderBook();
        MatchingEngine engine = new MatchingEngine(book);

        List<Trade> trades = engine.match(buy(1, "100", "5"));

        assertTrue(trades.isEmpty()); //NO TRADES
        assertNotNull(book.bestBid()); //ORDER FELL INTO CUP
        assertEquals(0, book.bestBid().getKey().compareTo(new BigDecimal("100"))); //CHECK IF IT IS REALLY THERE
    }

    @Test
    void buyMatchesEqualAsk() {
        OrderBook book = new OrderBook();
        MatchingEngine engine = new MatchingEngine(book);

        book.addOrder(sell(1, "100", "5"));
        List<Trade> trades = engine.match(buy(2, "100", "5"));

        assertEquals(1, trades.size()); //TRADE CREATION CHECK
        assertEquals(0, trades.get(0).price().compareTo(new BigDecimal("100")));
        assertEquals(0, trades.get(0).quantity().compareTo(new BigDecimal("5")));

        assertNull(book.bestBid()); // BUYER SATISFIED, ORDER REMOVED
        assertNull(book.bestAsk()); // MAKER SATISFIED
    }

    @Test
    void buyEatsTwoAsks() {
        OrderBook book = new OrderBook();
        MatchingEngine engine = new MatchingEngine(book);

        book.addOrder(sell(1, "100", "3"));
        book.addOrder(sell(2, "101", "5"));
        List<Trade> trades = engine.match(buy(3, "101", "6"));   // REQUESTS 6 FOR 101 OR LESS

        assertEquals(2, trades.size());                // ONE - COMPLETELY | SECOND - PARTIALLY
        // FIRST: 3 for 100, SECOND: 3 for 101
        assertEquals(0, trades.get(0).quantity().compareTo(new BigDecimal("3")));
        assertEquals(0, trades.get(1).quantity().compareTo(new BigDecimal("3")));
        assertNotNull(book.bestAsk());
        assertEquals(0, book.bestAsk().getValue().peekOrder().getRemainingQuantity().compareTo(new BigDecimal("2")));
    }

    @Test
    void sellWithNoBidsRestsInBook() {
        OrderBook book = new OrderBook();
        MatchingEngine engine = new MatchingEngine(book);

        List<Trade> trades = engine.match(sell(1, "100", "5"));

        assertTrue(trades.isEmpty()); // NO BUYER - NO TRADES
        assertNotNull(book.bestAsk()); // SELLER FELL TO CUP (ASKS SIDE)
        assertEquals(0, book.bestAsk().getKey().compareTo(new BigDecimal("100")));
    }

    @Test
    void sellMatchesEqualBid() {
        OrderBook book = new OrderBook();
        MatchingEngine engine = new MatchingEngine(book);

        book.addOrder(buy(1, "100", "5")); // BUYER STAYS IN THE CUP
        List<Trade> trades = engine.match(sell(2, "100", "5")); // SELLER FOR THE SAME QNTTY

        assertEquals(1, trades.size());
        assertEquals(0, trades.get(0).price().compareTo(new BigDecimal("100")));
        assertEquals(0, trades.get(0).quantity().compareTo(new BigDecimal("5")));
        assertEquals(1, trades.get(0).buyOrderId());
        assertEquals(2, trades.get(0).sellOrderId());
        assertNull(book.bestBid()); // BUYER SATISFIED
        assertNull(book.bestAsk()); // SELLER SATISFIED - NO CUP FALLING
    }

    @Test
    void marketBuyPartialFillCancelsRemainder() {
        OrderBook book = new OrderBook();
        MatchingEngine engine = new MatchingEngine(book);

        book.addOrder(sell(1, "100", "2")); // ONLY 2 IN CUP
        Order market = marketBuy(3, "5"); // market wants 5
        List<Trade> trades = engine.match(market);

        assertEquals(1, trades.size()); // MATCH 2
        assertEquals(0, market.getRemainingQuantity().compareTo(new BigDecimal("3"))); // LEFT 3
        assertEquals(Status.CANCELLED, market.getStatus()); // REMAINING CANCELED
        assertNull(book.bestBid()); // MARKET not IN THE CUP
    }
}
