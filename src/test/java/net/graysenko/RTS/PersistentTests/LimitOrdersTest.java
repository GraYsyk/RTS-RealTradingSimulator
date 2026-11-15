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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

public class LimitOrdersTest extends AbstractPersistentConfigurationHelper{

	@Test
	void limitBuyRestsAndFreezesPoints() {
		User buyer = createUserWithPoints(BigDecimal.valueOf(1000));
		Asset asset = setupTestingAsset();

		OrderEntity orderEntity = orderService.placeOrder(
				buyer,
				asset,
				Side.BUY,
				OrderType.LIMIT,
				BigDecimal.valueOf(100),
				BigDecimal.valueOf(6)
		);

		Wallet buyerWallet = findWalletByUserAndAsset(buyer, pointsAsset);

		assertThat(buyerWallet.getAvailable()).isEqualByComparingTo("400");
		assertThat(buyerWallet.getLocked()).isEqualByComparingTo("600");
		assertThat(orderEntity.getStatus()).isEqualTo(Status.OPEN);
		assertThat(orderEntity.getRemainingQuantity()).isEqualByComparingTo("6");
	}

	@Test
	void limitSellRestsAndFreezesAsset() {
		Asset asset = setupTestingAsset();
		User seller = createUserWithAsset(BigDecimal.valueOf(5), asset);

		OrderEntity orderEntity = orderService.placeOrder(
				seller,
				asset,
				Side.SELL,
				OrderType.LIMIT,
				BigDecimal.valueOf(1000),
				BigDecimal.valueOf(3)
		);

		Wallet sellerWallet = findWalletByUserAndAsset(seller, asset);

		assertThat(sellerWallet.getAvailable()).isEqualByComparingTo("2");
		assertThat(sellerWallet.getLocked()).isEqualByComparingTo("3");
		assertThat(orderEntity.getStatus()).isEqualTo(Status.OPEN);
		assertThat(orderEntity.getRemainingQuantity()).isEqualByComparingTo("3");
	}

	@Test
	void crossingLimitsTradeAtMakerPrice() {
		Asset asset = setupTestingAsset();
		User seller = createUserWithAsset(BigDecimal.valueOf(1), asset);
		User buyer = createUserWithPoints(BigDecimal.valueOf(100));

		OrderEntity sellOrderEntity = orderService.placeOrder(
				seller,
				asset,
				Side.SELL,
				OrderType.LIMIT,
				BigDecimal.valueOf(90),
				BigDecimal.valueOf(1)
		);

		OrderEntity buyerOrderEntity = orderService.placeOrder(
				buyer,
				asset,
				Side.BUY,
				OrderType.LIMIT,
				BigDecimal.valueOf(100),
				BigDecimal.valueOf(1)
		);

		sellOrderEntity = fetchFreshOrderEntity(sellOrderEntity);

		Wallet sellerPointsWallet = findWalletByUserAndAsset(seller, pointsAsset);
		Wallet sellerAssetWallet = findWalletByUserAndAsset(seller, asset);

		Wallet buyerPointsWallet = findWalletByUserAndAsset(buyer, pointsAsset);
		Wallet buyerAssetWallet = findWalletByUserAndAsset(buyer, asset);

		assertThat(sellOrderEntity.getStatus()).isEqualTo(Status.FILLED);
		assertThat(buyerOrderEntity.getStatus()).isEqualTo(Status.FILLED);

		assertThat(sellerPointsWallet.getAvailable()).isEqualByComparingTo("90");
		assertThat(sellerAssetWallet.getAvailable()).isEqualByComparingTo("0");
		assertThat(sellerAssetWallet.getLocked()).isEqualByComparingTo("0");

		assertThat(buyerPointsWallet.getAvailable()).isEqualByComparingTo("10");
		assertThat(buyerPointsWallet.getLocked()).isEqualByComparingTo("0");
		assertThat(buyerAssetWallet.getAvailable()).isEqualByComparingTo("1");
	}

	@Test
	void buyLimitRefundsPriceImprovement() {
		Asset asset = setupTestingAsset();
		User seller1 = createUserWithAsset(BigDecimal.valueOf(1), asset);
		User seller2 = createUserWithAsset(BigDecimal.valueOf(1), asset);
		User buyer = createUserWithPoints(BigDecimal.valueOf(1000));

		OrderEntity firstSellOrderEntity = orderService.placeOrder(
				seller1,
				asset,
				Side.SELL,
				OrderType.LIMIT,
				BigDecimal.valueOf(90),
				BigDecimal.valueOf(1)
		);

		OrderEntity secondSellOrderEntity = orderService.placeOrder(
				seller2,
				asset,
				Side.SELL,
				OrderType.LIMIT,
				BigDecimal.valueOf(80),
				BigDecimal.valueOf(1)
		);

		OrderEntity buyerOrderEntity = orderService.placeOrder(
				buyer,
				asset,
				Side.BUY,
				OrderType.LIMIT,
				BigDecimal.valueOf(100),
				BigDecimal.valueOf(2)
		);
		firstSellOrderEntity = fetchFreshOrderEntity(firstSellOrderEntity);
		secondSellOrderEntity = fetchFreshOrderEntity(secondSellOrderEntity);

		Wallet buyerPointsWallet = findWalletByUserAndAsset(buyer, pointsAsset);
		Wallet buyerAssetWallet = findWalletByUserAndAsset(buyer, asset);

		assertThat(buyerPointsWallet.getAvailable()).isEqualByComparingTo("830");
		assertThat(buyerPointsWallet.getLocked()).isEqualByComparingTo("0");
		assertThat(buyerAssetWallet.getAvailable()).isEqualByComparingTo("2");

		assertThat(firstSellOrderEntity.getStatus()).isEqualTo(Status.FILLED);
		assertThat(secondSellOrderEntity.getStatus()).isEqualTo(Status.FILLED);
		assertThat(buyerOrderEntity.getStatus()).isEqualTo(Status.FILLED);
	}

	@Test
	void partialFillStaysPartiallyFilled() {
		Asset asset = setupTestingAsset();
		User seller = createUserWithAsset(BigDecimal.valueOf(1), asset);
		User buyer = createUserWithPoints(BigDecimal.valueOf(1000));

		OrderEntity sellOrderEntity = orderService.placeOrder(
				seller,
				asset,
				Side.SELL,
				OrderType.LIMIT,
				BigDecimal.valueOf(100),
				BigDecimal.valueOf(1)
		);

		OrderEntity buyerOrderEntity = orderService.placeOrder(
				buyer,
				asset,
				Side.BUY,
				OrderType.LIMIT,
				BigDecimal.valueOf(100),
				BigDecimal.valueOf(3)
		);

		sellOrderEntity = fetchFreshOrderEntity(sellOrderEntity);
		buyerOrderEntity = fetchFreshOrderEntity(buyerOrderEntity);

		Wallet sellerPointsWallet = findWalletByUserAndAsset(seller, pointsAsset);
		Wallet sellerAssetWallet = findWalletByUserAndAsset(seller, asset);
		Wallet buyerPointsWallet = findWalletByUserAndAsset(buyer, pointsAsset);
		Wallet buyerAssetWallet = findWalletByUserAndAsset(buyer, asset);

		assertThat(buyerPointsWallet.getAvailable()).isEqualByComparingTo("700");
		assertThat(buyerPointsWallet.getLocked()).isEqualByComparingTo("200");
		assertThat(buyerAssetWallet.getAvailable()).isEqualByComparingTo("1");

		assertThat(sellerAssetWallet.getAvailable()).isEqualByComparingTo("0");
		assertThat(sellerAssetWallet.getLocked()).isEqualByComparingTo("0");
		assertThat(sellerPointsWallet.getAvailable()).isEqualByComparingTo("100");

		assertThat(sellOrderEntity.getStatus()).isEqualTo(Status.FILLED);
		assertThat(buyerOrderEntity.getStatus()).isEqualTo(Status.PARTIALLY_FILLED);
		assertThat(buyerOrderEntity.getRemainingQuantity()).isEqualByComparingTo("2");
	}

	@Test
	void limitBuyInsufficientFunds() {
		Asset asset = setupTestingAsset();
		User buyer = createUserWithPoints(BigDecimal.valueOf(100));

		assertThrows(InsufficientFundsException.class, () -> orderService.placeOrder(buyer,
				asset,
				Side.BUY,
				OrderType.LIMIT,
				BigDecimal.valueOf(100),
				BigDecimal.valueOf(2))
		);

		Wallet buyerWallet = findWalletByUserAndAsset(buyer, pointsAsset);

		assertFalse(orderRepository.existsByUser(buyer));
		assertThat(buyerWallet.getAvailable()).isEqualByComparingTo("100");
		assertThat(buyerWallet.getLocked()).isEqualByComparingTo("0");
	}
}
