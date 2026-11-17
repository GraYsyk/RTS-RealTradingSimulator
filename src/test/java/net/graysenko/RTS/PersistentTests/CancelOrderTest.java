package net.graysenko.RTS.PersistentTests;

import net.graysenko.RTS.MatchingEngine.Enums.OrderType;
import net.graysenko.RTS.MatchingEngine.Enums.Side;
import net.graysenko.RTS.MatchingEngine.Enums.Status;
import net.graysenko.RTS.Persistent.Entities.Asset;
import net.graysenko.RTS.Persistent.Entities.OrderEntity;
import net.graysenko.RTS.Persistent.Entities.User;
import net.graysenko.RTS.Persistent.Entities.Wallet;
import net.graysenko.RTS.Persistent.Exceptions.OrderNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CancelOrderTest extends AbstractPersistentConfigurationHelper{

    @Test
    void cancelBuyUnlocksPoints() {
        Asset asset = setupTestingAsset();
        User buyer = createUserWithPoints(BigDecimal.valueOf(1000));

        OrderEntity buyOrderEntity = orderService.placeOrder(
                buyer,
                asset,
                Side.BUY,
                OrderType.LIMIT,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(2)
        );
        Status orderStatus = orderService.cancelOrder(buyOrderEntity.getId());

        buyOrderEntity = fetchFreshOrderEntity(buyOrderEntity);
        Wallet buyerPointsWallet = findWalletByUserAndAsset(buyer, pointsAsset);

        assertThat(orderStatus).isEqualTo(Status.CANCELLED);
        assertThat(buyOrderEntity.getStatus()).isEqualTo(Status.CANCELLED);

        assertThat(buyerPointsWallet.getAvailable()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(buyerPointsWallet.getLocked()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void cancelSellUnlocksAsset() {
        Asset asset = setupTestingAsset();
        User seller = createUserWithAsset(BigDecimal.valueOf(5), asset);

        OrderEntity sellOrderEntity = orderService.placeOrder(
                seller,
                asset,
                Side.SELL,
                OrderType.LIMIT,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(3)
        );
        Status orderStatus = orderService.cancelOrder(sellOrderEntity.getId());

        sellOrderEntity = fetchFreshOrderEntity(sellOrderEntity);
        Wallet sellerAssetWallet = findWalletByUserAndAsset(seller, asset);

        assertThat(orderStatus).isEqualTo(Status.CANCELLED);
        assertThat(sellOrderEntity.getStatus()).isEqualTo(Status.CANCELLED);
        assertThat(sellerAssetWallet.getAvailable()).isEqualByComparingTo(BigDecimal.valueOf(5));
        assertThat(sellerAssetWallet.getLocked()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void cancelledOrderNotMatched() {
        Asset asset = setupTestingAsset();
        User buyer = createUserWithPoints(BigDecimal.valueOf(100));
        User seller = createUserWithAsset(BigDecimal.valueOf(1), asset);

        OrderEntity buyOrderEntity = orderService.placeOrder(
                buyer,
                asset,
                Side.BUY,
                OrderType.LIMIT,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1)
        );
        Status buyOrderStatus = orderService.cancelOrder(buyOrderEntity.getId());

        OrderEntity sellOrderEntity = orderService.placeOrder(
                seller,
                asset,
                Side.SELL,
                OrderType.LIMIT,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1)
        );

        Wallet sellerAssetWallet = findWalletByUserAndAsset(seller, asset);
        Wallet buyerPointsWallet = findWalletByUserAndAsset(buyer, pointsAsset);

        sellOrderEntity = fetchFreshOrderEntity(sellOrderEntity);

        assertThat(buyOrderStatus).isEqualTo(Status.CANCELLED);
        assertThat(sellOrderEntity.getStatus()).isEqualTo(Status.OPEN);
        assertThat(sellerAssetWallet.getAvailable()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(sellerAssetWallet.getLocked()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(buyerPointsWallet.getAvailable()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(buyerPointsWallet.getLocked()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void cancelFilledOrderChangesNothing() {
        Asset asset = setupTestingAsset();
        User buyer = createUserWithPoints(BigDecimal.valueOf(100));
        User seller = createUserWithAsset(BigDecimal.valueOf(1), asset);

        OrderEntity buyOrderEntity = orderService.placeOrder(
                buyer,
                asset,
                Side.BUY,
                OrderType.LIMIT,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1)
        );

        OrderEntity sellOrderEntity = orderService.placeOrder(
                seller,
                asset,
                Side.SELL,
                OrderType.LIMIT,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1)
        );
        Status buyOrderStatus = orderService.cancelOrder(buyOrderEntity.getId());

        Wallet buyerPointsWallet = findWalletByUserAndAsset(buyer, pointsAsset);
        Wallet buyerAssetWallet = findWalletByUserAndAsset(buyer, asset);

        sellOrderEntity = fetchFreshOrderEntity(sellOrderEntity);

        assertThat(buyOrderStatus).isEqualTo(Status.FILLED);
        assertThat(sellOrderEntity.getStatus()).isEqualTo(Status.FILLED);
        assertThat(buyerAssetWallet.getAvailable()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(buyerAssetWallet.getLocked()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(buyerPointsWallet.getAvailable()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(buyerPointsWallet.getLocked()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void cancelPartiallyFilledUnlocksOnlyRemainder() {
        Asset asset = setupTestingAsset();
        User buyer = createUserWithPoints(BigDecimal.valueOf(1000));
        User seller = createUserWithAsset(BigDecimal.valueOf(2), asset);

        OrderEntity buyOrderEntity = orderService.placeOrder(
                buyer,
                asset,
                Side.BUY,
                OrderType.LIMIT,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(5)
        );

        OrderEntity sellOrderEntity = orderService.placeOrder(
                seller,
                asset,
                Side.SELL,
                OrderType.LIMIT,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(2)
        );
        Wallet buyerPointsWallet = findWalletByUserAndAsset(buyer, pointsAsset);

        assertThat(buyerPointsWallet.getAvailable()).isEqualByComparingTo("500");
        assertThat(buyerPointsWallet.getLocked()).isEqualByComparingTo("300");

        Status buyOrderStatus = orderService.cancelOrder(buyOrderEntity.getId());
        Wallet secondBuyerPointsWallet = findWalletByUserAndAsset(buyer, pointsAsset);
        Wallet secondBuyerAssetWallet = findWalletByUserAndAsset(buyer, asset);
        sellOrderEntity = fetchFreshOrderEntity(sellOrderEntity);
        assertThat(buyOrderStatus).isEqualTo(Status.CANCELLED);
        assertThat(sellOrderEntity.getStatus()).isEqualTo(Status.FILLED);
        assertThat(secondBuyerAssetWallet.getAvailable()).isEqualByComparingTo(BigDecimal.TWO);
        assertThat(secondBuyerPointsWallet.getAvailable()).isEqualByComparingTo("800");
        assertThat(secondBuyerPointsWallet.getLocked()).isEqualByComparingTo("0");
    }

    @Test
    void cancelTwiceDoesNotDoubleUnlock() {
        Asset asset = setupTestingAsset();
        User buyer = createUserWithPoints(BigDecimal.valueOf(100));

        OrderEntity buyOrderEntity = orderService.placeOrder(
                buyer,
                asset,
                Side.BUY,
                OrderType.LIMIT,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1)
        );
        Status buyOrderStatus = orderService.cancelOrder(buyOrderEntity.getId());
        Wallet buyerPointsWallet = findWalletByUserAndAsset(buyer, pointsAsset);
        assertThat(buyOrderStatus).isEqualTo(Status.CANCELLED);
        assertThat(buyerPointsWallet.getAvailable()).isEqualByComparingTo("100");
        assertThat(buyerPointsWallet.getLocked()).isEqualByComparingTo(BigDecimal.ZERO);

        Status secondBuyOrderStatus = orderService.cancelOrder(buyOrderEntity.getId());
        Wallet secondBuyerPointsWallet = findWalletByUserAndAsset(buyer, pointsAsset);
        assertThat(secondBuyOrderStatus).isEqualTo(Status.CANCELLED);
        assertThat(secondBuyerPointsWallet.getAvailable()).isEqualByComparingTo("100");
        assertThat(secondBuyerPointsWallet.getLocked()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void cancelUnknownOrderThrows() {
        assertThrows(OrderNotFoundException.class, () -> orderService.cancelOrder(-1));
    }
}
