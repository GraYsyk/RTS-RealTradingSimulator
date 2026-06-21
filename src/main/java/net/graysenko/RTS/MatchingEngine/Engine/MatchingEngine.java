package net.graysenko.RTS.MatchingEngine.Engine;

import net.graysenko.RTS.MatchingEngine.Enums.OrderType;
import net.graysenko.RTS.MatchingEngine.Enums.Side;
import net.graysenko.RTS.MatchingEngine.POJO.Order;
import net.graysenko.RTS.MatchingEngine.POJO.PriceLevel;
import net.graysenko.RTS.MatchingEngine.Records.Trade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MatchingEngine {

    private final OrderBook orderBook;

    private long tradeIdSeq = 0;

    public MatchingEngine(OrderBook orderBook) {
        this.orderBook = orderBook;
    }

    public List<Trade> match(Order incoming) {
        List<Trade> trades = new ArrayList<>();

        //BUY
        if (incoming.getSide() == Side.BUY) {
            while (!incoming.isFilled()) {
                var ask = orderBook.bestAsk();
                if (ask == null) break;
                if (incoming.getOrderType() == OrderType.LIMIT &&
                        ask.getKey().compareTo(incoming.getPrice()) > 0) break;

                //MATCH
                PriceLevel askLevel = ask.getValue();
                Order maker = askLevel.peekOrder();

                BigDecimal totalQty = incoming.getRemainingQuantity().min(maker.getRemainingQuantity());
                BigDecimal tradePrice = ask.getKey();

                maker.fill(totalQty);
                incoming.fill(totalQty);

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
    }
}
