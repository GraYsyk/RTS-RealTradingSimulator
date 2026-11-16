package net.graysenko.RTS.PersistentTests;

import net.graysenko.RTS.MatchingEngine.Enums.OrderType;
import net.graysenko.RTS.MatchingEngine.Enums.Side;
import net.graysenko.RTS.MatchingEngine.Enums.Status;
import net.graysenko.RTS.Persistent.Entities.Asset;
import net.graysenko.RTS.Persistent.Entities.OrderEntity;
import net.graysenko.RTS.Persistent.Entities.User;
import net.graysenko.RTS.Persistent.Entities.Wallet;
import net.graysenko.RTS.Persistent.Exceptions.InsufficientFundsException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MarketOrdersTest extends AbstractPersistentConfigurationHelper {
    @Test
    void marketBuyFullyFilled(){
        Asset asset = setupTestingAsset();
        User seller = createUserWithAsset(BigDecimal.TWO, asset);
        User buyer = createUserWithPoints(BigDecimal.valueOf(1000));

        OrderEntity sellOrderEntity = orderService.placeOrder(
                seller,
                asset,
                Side.SELL,
                OrderType.LIMIT,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(2)
        );

        OrderEntity buyOrderEntity = orderService.placeOrder(
                buyer,
                asset,
                Side.BUY,
                OrderType.MARKET,
                null,
                BigDecimal.valueOf(2)
        );

        sellOrderEntity = fetchFreshOrderEntity(sellOrderEntity);
        buyOrderEntity = fetchFreshOrderEntity(buyOrderEntity);

        Wallet buyPointsWallet = findWalletByUserAndAsset(buyer, pointsAsset);
        Wallet sellPointsWallet = findWalletByUserAndAsset(seller, pointsAsset);
        Wallet buyAssetWallet = findWalletByUserAndAsset(buyer,asset);
        Wallet sellAssetWallet = findWalletByUserAndAsset(seller,asset);

        assertThat(sellOrderEntity.getStatus()).isEqualTo(Status.FILLED);
        assertThat(buyOrderEntity.getStatus()).isEqualTo(Status.FILLED);

        assertThat(buyPointsWallet.getAvailable()).isEqualByComparingTo(BigDecimal.valueOf(800));
        assertThat(buyPointsWallet.getLocked()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(sellPointsWallet.getAvailable()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(buyAssetWallet.getAvailable()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(sellAssetWallet.getAvailable()).isEqualByComparingTo(BigDecimal.valueOf(0));
    }

    @Test
    void marketBuyPartialIsCancelled() {
        Asset asset = setupTestingAsset();
        User seller = createUserWithAsset(BigDecimal.ONE, asset);
        User seller2 = createUserWithAsset(BigDecimal.ONE, asset);
        User buyer = createUserWithPoints(BigDecimal.valueOf(1000));

        OrderEntity sellOrderEntity = orderService.placeOrder(
                seller,
                asset,
                Side.SELL,
                OrderType.LIMIT,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1)
        );

        OrderEntity buyOrderEntity = orderService.placeOrder(
                buyer,
                asset,
                Side.BUY,
                OrderType.MARKET,
                null,
                BigDecimal.valueOf(3)
        );

        OrderEntity secondSellOrderEntity = orderService.placeOrder(
                seller2,
                asset,
                Side.SELL,
                OrderType.LIMIT,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1)
        );

        sellOrderEntity = fetchFreshOrderEntity(sellOrderEntity);
        secondSellOrderEntity = fetchFreshOrderEntity(secondSellOrderEntity);
        buyOrderEntity = fetchFreshOrderEntity(buyOrderEntity);

        Wallet buyPointsWallet = findWalletByUserAndAsset(buyer, pointsAsset);
        Wallet sellPointsWallet = findWalletByUserAndAsset(seller, pointsAsset);
        Wallet buyAssetWallet = findWalletByUserAndAsset(buyer,asset);
        Wallet sellAssetWallet = findWalletByUserAndAsset(seller,asset);

        assertThat(sellOrderEntity.getStatus()).isEqualTo(Status.FILLED);
        assertThat(buyOrderEntity.getStatus()).isEqualTo(Status.CANCELLED);
        assertThat(secondSellOrderEntity.getStatus()).isEqualTo(Status.OPEN);

        assertThat(buyOrderEntity.getRemainingQuantity()).isEqualByComparingTo(BigDecimal.TWO);

        assertThat(buyPointsWallet.getAvailable()).isEqualByComparingTo(BigDecimal.valueOf(900));
        assertThat(buyPointsWallet.getLocked()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(sellPointsWallet.getAvailable()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(buyAssetWallet.getAvailable()).isEqualByComparingTo(BigDecimal.valueOf(1));
        assertThat(sellAssetWallet.getAvailable()).isEqualByComparingTo(BigDecimal.valueOf(0));
    }

    @Test
    void marketBuyLimitedByBudget() {
        Asset asset = setupTestingAsset();
        User seller = createUserWithAsset(BigDecimal.valueOf(2), asset);
        User seller2 = createUserWithAsset(BigDecimal.valueOf(3), asset);
        User buyer = createUserWithPoints(BigDecimal.valueOf(3000));

        OrderEntity sellOrderEntity = orderService.placeOrder(
                seller,
                asset,
                Side.SELL,
                OrderType.LIMIT,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(2)
        );

        OrderEntity secondSellOrderEntity = orderService.placeOrder(
                seller2,
                asset,
                Side.SELL,
                OrderType.LIMIT,
                BigDecimal.valueOf(1200),
                BigDecimal.valueOf(3)
        );

        OrderEntity buyOrderEntity = orderService.placeOrder(
                buyer,
                asset,
                Side.BUY,
                OrderType.MARKET,
                null,
                BigDecimal.valueOf(5)
        );

        sellOrderEntity = fetchFreshOrderEntity(sellOrderEntity);
        secondSellOrderEntity = fetchFreshOrderEntity(secondSellOrderEntity);
        buyOrderEntity = fetchFreshOrderEntity(buyOrderEntity);

        assertThat(sellOrderEntity.getStatus()).isEqualTo(Status.FILLED);
        assertThat(secondSellOrderEntity.getStatus()).isEqualTo(Status.PARTIALLY_FILLED);
        assertThat(buyOrderEntity.getStatus()).isEqualTo(Status.CANCELLED);

        Wallet buyPointsWallet = findWalletByUserAndAsset(buyer, pointsAsset);
        Wallet buyAssetWallet = findWalletByUserAndAsset(buyer,asset);

        assertThat(buyAssetWallet.getAvailable()).isEqualByComparingTo("2.8333333333");
        assertThat(buyPointsWallet.getAvailable()).isEqualByComparingTo("0.00000004");
        assertThat(buyPointsWallet.getLocked()).isEqualByComparingTo(BigDecimal.valueOf(0));
    }

    @Test
    void marketSellInsufficientAsset() {
        Asset asset = setupTestingAsset();
        User seller = createUserWithAsset(BigDecimal.valueOf(1), asset);
        User buyer = createUserWithPoints(BigDecimal.valueOf(200));

        OrderEntity buyOrderEntity = orderService.placeOrder(
                buyer,
                asset,
                Side.BUY,
                OrderType.LIMIT,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(2)
        );

        assertThrows( InsufficientFundsException.class, () -> orderService.placeOrder(
                seller,
                asset,
                Side.SELL,
                OrderType.MARKET,
                null,
                BigDecimal.valueOf(2)
        ));

        buyOrderEntity = fetchFreshOrderEntity(buyOrderEntity);

        Wallet buyPointsWallet = findWalletByUserAndAsset(buyer, pointsAsset);
        Wallet sellAssetWallet = findWalletByUserAndAsset(seller,asset);

        assertThat(buyOrderEntity.getStatus()).isEqualTo(Status.OPEN);
        assertThat(buyPointsWallet.getLocked()).isEqualByComparingTo(BigDecimal.valueOf(200));

        assertThat(sellAssetWallet.getAvailable()).isEqualByComparingTo(BigDecimal.ONE);
        assertFalse(orderRepository.existsByUser(seller));
    }

    @Test
    void marketOnEmptyBookIsCancelled() {
        Asset asset = setupTestingAsset();
        User buyer = createUserWithPoints(BigDecimal.valueOf(1000));

        OrderEntity buyOrderEntity = orderService.placeOrder(
                buyer,
                asset,
                Side.BUY,
                OrderType.MARKET,
                null,
                BigDecimal.valueOf(1)
        );

        buyOrderEntity = fetchFreshOrderEntity(buyOrderEntity);

        Wallet buyPointsWallet = findWalletByUserAndAsset(buyer, pointsAsset);

        assertThat(buyOrderEntity.getStatus()).isEqualTo(Status.CANCELLED);
        assertThat(buyOrderEntity.getRemainingQuantity()).isEqualByComparingTo(BigDecimal.valueOf(1));

        assertThat(buyPointsWallet.getAvailable()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(buyPointsWallet.getLocked()).isEqualByComparingTo(BigDecimal.valueOf(0));
    }
}
