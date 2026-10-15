package net.graysenko.RTS.Persistent.Utils;

import lombok.AllArgsConstructor;
import net.graysenko.RTS.MatchingEngine.Engine.MatchingEngine;
import net.graysenko.RTS.MatchingEngine.Engine.OrderBook;
import net.graysenko.RTS.MatchingEngine.Enums.Status;
import net.graysenko.RTS.MatchingEngine.POJO.Order;
import net.graysenko.RTS.Persistent.Entities.Asset;
import net.graysenko.RTS.Persistent.Entities.OrderEntity;
import net.graysenko.RTS.Persistent.Enums.AssetStatus;
import net.graysenko.RTS.Persistent.Enums.CurrencyType;
import net.graysenko.RTS.Persistent.Repositories.AssetRepository;
import net.graysenko.RTS.Persistent.Repositories.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class MatchingEngineRegistry implements CommandLineRunner {

    private final OrderRepository orderRepository;
    private final AssetRepository assetRepository;

    private final ConcurrentHashMap<Long, MatchingEngine> engines = new ConcurrentHashMap<>();

    public MatchingEngine getMatchingEngine(long assetId) {
        MatchingEngine me = engines.getOrDefault(assetId, null);
        if(me == null) throw new RuntimeException("No matching engine found for asset id " + assetId);
        return me;
    }


    @Override
    public void run(String... args) throws Exception {
        seedBasicStartupAssets("POINTS", "Points",CurrencyType.FIAT, "someUrl");
        seedBasicStartupAssets("BTC", "Bitcoin",CurrencyType.CRYPTO, "someUrl");
        createEnginesForApprovedAssets();
        restoreOpenOrders();
    }

    private void seedBasicStartupAssets(String currencySymbol, String currencyName,
                                        CurrencyType currencyType, String iconUrl) {
        Asset asset = assetRepository.findBySymbol(currencySymbol).orElse(null);
        if(asset == null) asset = new Asset(
                null,
                currencySymbol,
                currencyName,
                currencyType,
                iconUrl
        );
        asset.setStatus(AssetStatus.APPROVED);
        assetRepository.save(asset);
    }

    private void createEnginesForApprovedAssets() {
        List<Asset> approvedAssets = assetRepository.findAssetsByStatus(AssetStatus.APPROVED);
        for(Asset asset : approvedAssets) {
            engines.put(asset.getId(), new MatchingEngine(new OrderBook()));
        }
    }

    private void restoreOpenOrders() {
        List<OrderEntity> openOrders = orderRepository.findByStatusIn(List.of(Status.OPEN, Status.PARTIALLY_FILLED));
        for(OrderEntity order : openOrders) {
            MatchingEngine me = getMatchingEngine(order.getAsset().getId());
            Order domainOrder = orderfy(order);
            me.getOrderBook().addOrder(domainOrder);
        }
    }

    public Order orderfy(OrderEntity order) {
        if (order == null) throw new RuntimeException("Order is null");
        return new Order(
                order.getId(),
                order.getUser().getId(),
                order.getSide(),
                order.getOrderType(),
                order.getPrice(),
                order.getQuantity(),
                order.getRemainingQuantity(),
                order.getCreatedAt(),
                order.getStatus()
        );
    }
}
