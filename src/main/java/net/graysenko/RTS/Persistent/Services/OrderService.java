package net.graysenko.RTS.Persistent.Services;

import lombok.RequiredArgsConstructor;
import net.graysenko.RTS.MatchingEngine.Engine.MatchingEngine;
import net.graysenko.RTS.MatchingEngine.Enums.OrderType;
import net.graysenko.RTS.MatchingEngine.Enums.Side;
import net.graysenko.RTS.MatchingEngine.Enums.Status;
import net.graysenko.RTS.MatchingEngine.POJO.Order;
import net.graysenko.RTS.MatchingEngine.Records.Trade;
import net.graysenko.RTS.Persistent.Entities.*;
import net.graysenko.RTS.Persistent.Repositories.OrderRepository;
import net.graysenko.RTS.Persistent.Utils.MatchingEngineRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderService {

    private final OrderRepository orderRepository;
    private final MatchingEngineRegistry matchingEngineRegistry;
    private final WalletService walletService;
    private final TradeService tradeService;

    @Transactional
    public OrderEntity placeOrder(User user, Asset asset, Side side,
                           OrderType orderType, BigDecimal price,
                           BigDecimal quantity) {
        if (price == null && orderType != OrderType.MARKET)
            throw new IllegalArgumentException("Price cannot be null if OrderType isn't MARKET");
        OrderEntity orderEntity = new OrderEntity(
                user, side, orderType, price, quantity,
                asset, quantity, Status.OPEN
        );
        if (orderType != OrderType.MARKET) {
            walletService.handleUserOrderCreation(user, orderEntity, asset);
        }
        orderEntity = orderRepository.save(orderEntity);

        MatchingEngine me = matchingEngineRegistry.getMatchingEngine(asset.getId());
        Order order = matchingEngineRegistry.orderfy(orderEntity);

        List<Trade> domainTrades = me.match(order);

        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (Trade trade : domainTrades) {
            OrderEntity buyOrder = getOrderById(trade.buyOrderId());
            OrderEntity sellOrder = getOrderById(trade.sellOrderId());

            buyOrder.fill(trade.quantity());
            sellOrder.fill(trade.quantity());

            totalQty = totalQty.add(trade.quantity());
            totalPrice = totalPrice.add(trade.price().multiply(trade.quantity()));

            orderRepository.save(buyOrder);
            orderRepository.save(sellOrder);

            TradeEntity tradeEntity = tradeService.tradify(trade, asset);

            walletService.handleTradeActions(tradeEntity, buyOrder, sellOrder);
        }

        if (side == Side.BUY && orderType == OrderType.LIMIT) {
            BigDecimal diff = totalQty.multiply(price).subtract(totalPrice);
            if (diff.signum() > 0) {
                walletService.unlock(walletService.findByUserAndAsset(user, asset), diff);
            }
        }

        return getOrderById(orderEntity.getId());
    }

    @Transactional
    public void cancelOrder(OrderEntity orderEntity) {
        // TODO: ОБРАТНАЯ СВЯЗЬ ЧТО ЗАКАЗ УЖЕ НЕ ЛИКВИДЕН
        if (orderEntity.getStatus().equals(Status.CANCELLED) || orderEntity.getStatus().equals(Status.FILLED)) return;

        if (orderEntity.getSide().equals(Side.BUY)) {
            Wallet walletOwner = walletService.getUserDefaultWallet(orderEntity.getUser());
            walletService.unlock(walletOwner, orderEntity.getPrice().multiply(orderEntity.getRemainingQuantity()));

        } else if (orderEntity.getSide().equals(Side.SELL)) {
            Wallet walletOwner = walletService.findByUserAndAsset(orderEntity.getUser(), orderEntity.getAsset());
            walletService.unlock(walletOwner, orderEntity.getRemainingQuantity());
        } else throw new IllegalArgumentException("Invalid order side type");

        orderEntity.cancel();
        orderRepository.save(orderEntity);
    }

    @Transactional(readOnly = true)
    public OrderEntity getOrderById(Long orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }

    @Transactional(readOnly = true)
    public User getUserFromOrderId(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(()
                -> new IllegalArgumentException("Order with id "+orderId+" wasn't found")).getUser();
    }
}
