package net.graysenko.RTS.Persistent.Services;

import lombok.RequiredArgsConstructor;
import net.graysenko.RTS.MatchingEngine.Engine.MatchingEngine;
import net.graysenko.RTS.MatchingEngine.Enums.OrderType;
import net.graysenko.RTS.MatchingEngine.Enums.Side;
import net.graysenko.RTS.MatchingEngine.Enums.Status;
import net.graysenko.RTS.MatchingEngine.POJO.Order;
import net.graysenko.RTS.MatchingEngine.Records.Trade;
import net.graysenko.RTS.Persistent.Entities.*;
import net.graysenko.RTS.Persistent.Exceptions.InsufficientFundsException;
import net.graysenko.RTS.Persistent.Exceptions.OrderNotFoundException;
import net.graysenko.RTS.Persistent.Repositories.OrderRepository;
import net.graysenko.RTS.Persistent.Utils.MatchingEngineRegistry;
import org.hibernate.PessimisticLockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderService {

    private final OrderRepository orderRepository;
    private final MatchingEngineRegistry matchingEngineRegistry;
    private final WalletService walletService;
    private final TradeService tradeService;

    @Retryable(includes = ConcurrencyFailureException.class,
            maxRetries = 3,
            multiplier = 2,
            jitter = 250,
            delay = 300)
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

        if (orderType == OrderType.MARKET && side == Side.SELL) {
            java.math.BigDecimal available = walletService.findByUserAndAsset(user, asset).getAvailable();
            if (quantity != null && available.compareTo(quantity) < 0) {
                throw new InsufficientFundsException("Quantity cannot be greater than available");
            }
        }

        orderEntity = orderRepository.save(orderEntity);

        MatchingEngine me = matchingEngineRegistry.getMatchingEngine(asset.getId());
        Order order = matchingEngineRegistry.orderfy(orderEntity);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    matchingEngineRegistry.rebuildEngine(asset.getId());
                }
            }
        });

        BigDecimal budget = null;
        if (side == Side.BUY && orderType == OrderType.MARKET) {
         budget = walletService.getUserDefaultWallet(user).getAvailable();
        }

        List<Trade> domainTrades = me.match(order, budget);

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

        if (orderType == OrderType.MARKET && !order.isFilled()) {
            orderEntity.cancel();
            orderRepository.save(orderEntity);
        }

        if (side == Side.BUY && orderType == OrderType.LIMIT) {
            BigDecimal diff = totalQty.multiply(price).subtract(totalPrice);
            if (diff.signum() > 0) {
                walletService.unlock(walletService.getUserDefaultWallet(user), diff);
            }
        }

        return getOrderById(orderEntity.getId());
    }

    @Transactional
    public Status cancelOrder(long orderId) {
        OrderEntity orderEntity = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException("Order id " + orderId + " not found"));
        if (orderEntity.getStatus().equals(Status.CANCELLED) || orderEntity.getStatus().equals(Status.FILLED)) return orderEntity.getStatus();

        if (orderEntity.getSide().equals(Side.BUY)) {
            Wallet walletOwner = walletService.getUserDefaultWallet(orderEntity.getUser());
            walletService.unlock(walletOwner, orderEntity.getPrice().multiply(orderEntity.getRemainingQuantity()));

        } else if (orderEntity.getSide().equals(Side.SELL)) {
            Wallet walletOwner = walletService.findByUserAndAsset(orderEntity.getUser(), orderEntity.getAsset());
            walletService.unlock(walletOwner, orderEntity.getRemainingQuantity());
        } else throw new IllegalArgumentException("Invalid order side type");

        orderEntity.cancel();
        orderRepository.save(orderEntity);

        final Side side = orderEntity.getSide();
        final BigDecimal price = orderEntity.getPrice();
        final long assetId = orderEntity.getAsset().getId();

        final Status status = orderEntity.getStatus();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                MatchingEngine me = matchingEngineRegistry.getMatchingEngine(assetId);
                me.cancel(side, orderId, price);
            }
        });

        return status;
    }

    @Transactional(readOnly = true)
    public OrderEntity getOrderById(Long orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }
}
