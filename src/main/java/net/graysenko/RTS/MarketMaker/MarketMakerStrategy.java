package net.graysenko.RTS.MarketMaker;

import net.graysenko.RTS.MarketMaker.Records.BotOrderSpec;
import net.graysenko.RTS.MarketMaker.Records.MarketMakerSettings;
import net.graysenko.RTS.MatchingEngine.Enums.Side;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class MarketMakerStrategy {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal MIN_GRID_STEP = new BigDecimal("0.01");

    public List<BotOrderSpec> buildLadder(BigDecimal price, MarketMakerSettings settings) {
        if (price == null || price.signum() <= 0) throw new IllegalArgumentException("Asset price must be greater than zero");

        BigDecimal g = gridStep(price, settings);
        BigDecimal halfZone = price.multiply(settings.zonePercent()).divide(HUNDRED.multiply(BigDecimal.TWO));
        BigDecimal sellStart = ceilToGrid(price.add(halfZone), g);
        BigDecimal buyStart = floorToGrid(price.subtract(halfZone), g);

        List<BotOrderSpec> sellList = new ArrayList<>();
        List<BotOrderSpec> buyList = new ArrayList<>();

        for (int i = 0; i < settings.levels(); i++) {
            BigDecimal sellPrice = sellStart.add(g.multiply(BigDecimal.valueOf(i)));
            BigDecimal buyPrice = buyStart.subtract(g.multiply(BigDecimal.valueOf(i)));

            //TODO: Add random order quantity and custom price for each order
            BotOrderSpec sellOrder = new BotOrderSpec(Side.SELL, sellPrice, settings.quantity());
            sellList.add(sellOrder);

            if (buyPrice.signum() > 0) {
                BotOrderSpec buyOrder = new BotOrderSpec(Side.BUY, buyPrice, settings.quantity());
                buyList.add(buyOrder);
            }
        }

        sellList = sellList.stream().sorted(Comparator.comparing(BotOrderSpec::price).reversed()).toList();

        return Stream.concat(sellList.stream(), buyList.stream()).toList();
    }

    private BigDecimal gridStep(BigDecimal price, MarketMakerSettings settings) {
        BigDecimal x = price.multiply(settings.gridStepPercent()).divide(HUNDRED);
        int n = (int) Math.floor(Math.log10(x.doubleValue()));
        BigDecimal g = BigDecimal.ONE.scaleByPowerOfTen(n);

        g = g.max(MIN_GRID_STEP);
        return g;
    }

    private BigDecimal floorToGrid(BigDecimal value, BigDecimal g) {
        return value.divide(g, 0, RoundingMode.FLOOR).multiply(g);
    }

    private BigDecimal ceilToGrid(BigDecimal value, BigDecimal g) {
        return value.divide(g, 0, RoundingMode.CEILING).multiply(g);
    }
}
