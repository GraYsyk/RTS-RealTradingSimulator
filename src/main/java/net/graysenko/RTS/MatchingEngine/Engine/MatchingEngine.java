package net.graysenko.RTS.MatchingEngine.Engine;

import lombok.Getter;
import net.graysenko.RTS.MatchingEngine.Enums.OrderType;
import net.graysenko.RTS.MatchingEngine.Enums.Side;
import net.graysenko.RTS.MatchingEngine.POJO.Order;
import net.graysenko.RTS.MatchingEngine.POJO.PriceLevel;
import net.graysenko.RTS.MatchingEngine.Records.Trade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class MatchingEngine {

    @Getter
    private final OrderBook orderBook;

    private long tradeIdSeq = 0;
    private final ReentrantLock lock = new ReentrantLock();

    private static final int QTY_SCALE = 10;

    public MatchingEngine(OrderBook orderBook) {
        this.orderBook = orderBook;
    }

    public List<Trade> match(Order incoming) {
        return match(incoming, null);
    }

    public List<Trade> match(Order incoming, BigDecimal maxQuoteAmount) {
        lock.lock();

        try {
            List<Trade> trades = new ArrayList<>();

            //BUY
            if (incoming.getSide() == Side.BUY) {
                BigDecimal quoteAmount = maxQuoteAmount;
                while (!incoming.isFilled()) {
                    var ask = orderBook.bestAsk();
                    if (ask == null) break;
                    if (incoming.getOrderType() == OrderType.LIMIT &&
                            ask.getKey().compareTo(incoming.getPrice()) > 0) break;

                    //MATCH
                    PriceLevel askLevel = ask.getValue();
                    Order maker = askLevel.peekOrder();

                    BigDecimal tradePrice = ask.getKey();

                    BigDecimal totalQty = incoming.getRemainingQuantity()
                            .min(maker.getRemainingQuantity());

                    if (quoteAmount != null) {
                        totalQty = totalQty.min(quoteAmount.divide(tradePrice, QTY_SCALE, RoundingMode.FLOOR));
                    }

                    if (totalQty.signum() == 0) break;

                    maker.fill(totalQty);
                    incoming.fill(totalQty);

                    if (quoteAmount != null) {
                        quoteAmount = quoteAmount.subtract(tradePrice.multiply(totalQty));
                    }

                    Trade trade = new Trade(
                            tradeIdSeq++,
                            incoming.getId(),
                            maker.getId(),
                            tradePrice,
                            totalQty,
                            Instant.now()
                    );
                    trades.add(trade);

                    if (maker.isFilled()) {
                        askLevel.pollOrder();
                        orderBook.removeLevelIfEmpty(ask.getKey(), Side.SELL);
                    }


                }
            } else if (incoming.getSide() == Side.SELL) {
                while (!incoming.isFilled()) {
                    var bid = orderBook.bestBid();
                    if (bid == null) break;
                    if (incoming.getOrderType() == OrderType.LIMIT &&
                            bid.getKey().compareTo(incoming.getPrice()) < 0) break;

                    //MATCH
                    PriceLevel bidLevel = bid.getValue();
                    Order maker = bidLevel.peekOrder();

                    BigDecimal totalQty = incoming.getRemainingQuantity().min(maker.getRemainingQuantity());
                    BigDecimal tradePrice = bid.getKey();

                    maker.fill(totalQty);
                    incoming.fill(totalQty);

                    Trade trade = new Trade(
                            tradeIdSeq++,
                            maker.getId(),
                            incoming.getId(),
                            tradePrice,
                            totalQty,
                            Instant.now()
                    );
                    trades.add(trade);

                    if (maker.isFilled()) {
                        bidLevel.pollOrder();
                        orderBook.removeLevelIfEmpty(bid.getKey(), Side.BUY);
                    }


                }
            }

            if (!incoming.isFilled()) {
                if (incoming.getOrderType() == OrderType.LIMIT) orderBook.addOrder(incoming);
                else incoming.cancel();
            }

            return trades;
        } finally {
            lock.unlock();
        }
    }

    public void cancel(Side side, long orderId, BigDecimal price) {
        lock.lock();
        try {
            orderBook.removeOrder(side, orderId, price);
        } finally {
            lock.unlock();
        }
    }
}
