package net.graysenko.RTS.MarketMakerTests;

import net.graysenko.RTS.MarketMaker.MarketMakerStrategy;
import net.graysenko.RTS.MarketMaker.Records.BotOrderSpec;
import net.graysenko.RTS.MarketMaker.Records.MarketMakerSettings;
import net.graysenko.RTS.MatchingEngine.Enums.Side;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MarketMakerStrategyTests {

    private final MarketMakerStrategy strategy = new MarketMakerStrategy();
    private final BigDecimal qty = new BigDecimal("0.01");
    // 3 levels per side, grid step 1%, user zone 2%
    private final MarketMakerSettings settings =
            new MarketMakerSettings(3, BigDecimal.ONE, BigDecimal.TWO, qty);

    // ---------- helpers ----------

    private List<BigDecimal> prices(List<BotOrderSpec> orders, Side side) {
        return orders.stream()
                .filter(o -> o.side() == side)
                .map(BotOrderSpec::price)
                .toList();
    }

    private List<BigDecimal> bd(String... values) {
        // BotOrderSpec normalizes prices to scale 2, so expected values are normalized the same way
        return java.util.Arrays.stream(values)
                .map(v -> new BigDecimal(v).setScale(2))
                .toList();
    }

    // ---------- core ladder ----------

    @Test
    public void buildsExpectedLadder() {
        List<BotOrderSpec> result = strategy.buildLadder(new BigDecimal("6400"), settings);

        assertThat(result).containsExactly(
                new BotOrderSpec(Side.SELL, new BigDecimal("6490"), qty),
                new BotOrderSpec(Side.SELL, new BigDecimal("6480"), qty),
                new BotOrderSpec(Side.SELL, new BigDecimal("6470"), qty),
                new BotOrderSpec(Side.BUY, new BigDecimal("6330"), qty),
                new BotOrderSpec(Side.BUY, new BigDecimal("6320"), qty),
                new BotOrderSpec(Side.BUY, new BigDecimal("6310"), qty)
        );
    }

    @Test
    public void buysBelowPriceSellsAbove() {
        BigDecimal basePrice = new BigDecimal("64477.16");
        List<BotOrderSpec> result = strategy.buildLadder(basePrice, settings);

        assertThat(result.stream().filter(r -> r.side() == Side.SELL))
                .allMatch(r -> r.price().compareTo(basePrice) > 0)
                .size().isEqualTo(3);
        assertThat(result.stream().filter(r -> r.side() == Side.BUY))
                .allMatch(r -> r.price().compareTo(basePrice) < 0)
                .size().isEqualTo(3);
    }

    @Test
    public void neighbourLevelsDifferByGridStep() {
        // 64477.16 * 1% = 644.77 -> grid step G = 100
        BigDecimal expectedStep = new BigDecimal("100");
        List<BotOrderSpec> result = strategy.buildLadder(new BigDecimal("64477.16"), settings);

        for (Side side : Side.values()) {
            List<BigDecimal> p = prices(result, side);
            for (int i = 1; i < p.size(); i++) {
                // both sides are ordered from high to low, so previous - current = G
                assertThat(p.get(i - 1).subtract(p.get(i)))
                        .as("%s step between %s and %s", side, p.get(i - 1), p.get(i))
                        .isEqualByComparingTo(expectedStep);
            }
        }
        // concrete values: edges 64477.16 +- 644.7716 -> 65200 / 63800
        assertThat(prices(result, Side.SELL)).containsExactlyElementsOf(bd("65400", "65300", "65200"));
        assertThat(prices(result, Side.BUY)).containsExactlyElementsOf(bd("63800", "63700", "63600"));
    }

    @Test
    public void quantityComesFromSettings() {
        List<BotOrderSpec> result = strategy.buildLadder(new BigDecimal("6400"), settings);

        assertThat(result)
                .isNotEmpty()
                .allMatch(o -> o.quantity().compareTo(qty) == 0);
    }

    // ---------- stability (the reason the grid exists) ----------

    @Test
    public void smallPriceMoveKeepsSameLevels() {
        List<BotOrderSpec> at6400 = strategy.buildLadder(new BigDecimal("6400"), settings);
        List<BotOrderSpec> at6403 = strategy.buildLadder(new BigDecimal("6403"), settings);

        assertThat(at6403).isEqualTo(at6400);
    }

    @Test
    public void priceMoveShiftsOneLevel() {
        List<BotOrderSpec> at6410 = strategy.buildLadder(new BigDecimal("6410"), settings);

        assertThat(prices(at6410, Side.SELL)).containsExactlyElementsOf(bd("6500", "6490", "6480"));
        assertThat(prices(at6410, Side.BUY)).containsExactlyElementsOf(bd("6340", "6330", "6320"));
    }

    // ---------- edge cases ----------

    @Test
    public void cheapCoinSkipsNonPositiveBuys() {
        MarketMakerSettings fiveLevels = new MarketMakerSettings(5, BigDecimal.ONE, BigDecimal.TWO, qty);
        List<BotOrderSpec> result = strategy.buildLadder(new BigDecimal("0.05"), fiveLevels);

        // G = 0.01 (minimum); 0.00 is skipped, so only 4 BUY levels
        assertThat(prices(result, Side.BUY)).containsExactlyElementsOf(bd("0.04", "0.03", "0.02", "0.01"));
        assertThat(prices(result, Side.SELL)).containsExactlyElementsOf(bd("0.10", "0.09", "0.08", "0.07", "0.06"));
    }

    @Test
    public void gridStepJumpsAtPowerOfTen() {
        // 9990 * 1% = 99.9 -> G = 10
        List<BotOrderSpec> below = strategy.buildLadder(new BigDecimal("9990"), settings);
        assertThat(prices(below, Side.SELL)).containsExactlyElementsOf(bd("10110", "10100", "10090"));
        assertThat(prices(below, Side.BUY)).containsExactlyElementsOf(bd("9890", "9880", "9870"));

        // 10010 * 1% = 100.1 -> G = 100
        List<BotOrderSpec> above = strategy.buildLadder(new BigDecimal("10010"), settings);
        assertThat(prices(above, Side.SELL)).containsExactlyElementsOf(bd("10400", "10300", "10200"));
        assertThat(prices(above, Side.BUY)).containsExactlyElementsOf(bd("9900", "9800", "9700"));
    }

    @Test
    public void rejectsInvalidPrice() {
        assertThatThrownBy(() -> strategy.buildLadder(BigDecimal.ZERO, settings))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> strategy.buildLadder(new BigDecimal("-1"), settings))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> strategy.buildLadder(null, settings))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- records ----------

    @Test
    public void settingsRejectZeroZone() {
        assertThatThrownBy(() -> new MarketMakerSettings(3, BigDecimal.ONE, BigDecimal.ZERO, qty))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void settingsRejectZeroLevels() {
        assertThatThrownBy(() -> new MarketMakerSettings(0, BigDecimal.ONE, BigDecimal.TWO, qty))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void specNormalizesScale() {
        // the way a price comes back from the DB column (scale 10)
        BotOrderSpec spec = new BotOrderSpec(Side.BUY, new BigDecimal("6330.0000000000"), qty);

        assertThat(spec.price()).isEqualTo(new BigDecimal("6330.00"));
        assertThat(spec.price().scale()).isEqualTo(2);
    }

    @Test
    public void specRejectsOffGridPrice() {
        assertThatThrownBy(() -> new BotOrderSpec(Side.SELL, new BigDecimal("6336.123"), qty))
                .isInstanceOf(ArithmeticException.class);
    }
}