package net.graysenko.RTS.Persistent.Services;

import net.graysenko.RTS.MatchingEngine.Enums.OrderType;
import net.graysenko.RTS.Persistent.Entities.*;
import net.graysenko.RTS.Persistent.Exceptions.InsufficientFundsException;
import net.graysenko.RTS.Persistent.Repositories.AssetRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import net.graysenko.RTS.MatchingEngine.Enums.Side;
import net.graysenko.RTS.Persistent.Repositories.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

import static java.lang.Math.max;
import static java.lang.Math.min;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class WalletService {

    private final WalletRepository walletRepository;
    private final AssetRepository assetRepository;

    @Transactional
    public void handleTradeActions(TradeEntity tradeEntity, OrderEntity buyOrder, OrderEntity sellerOrder) {
        User buyer = buyOrder.getUser();
        User seller = sellerOrder.getUser();
        User first = buyer.getId() < seller.getId() ? buyer : seller;
        User second = (first == buyer) ? seller : buyer;

        //deadlock fix
        Wallet FBW = getUserDefaultWallet(first);
        Wallet SBW = getUserDefaultWallet(second);
        Wallet FAW = findByUserAndAsset(first, buyOrder.getAsset());
        Wallet SeAW = findByUserAndAsset(second, buyOrder.getAsset());

        Wallet buyWallet = getUserDefaultWallet(buyOrder.getUser());
        Wallet sellWallet = getUserDefaultWallet(sellerOrder.getUser());

        //POINTS EXCHANGE
        var totalPricePoints = tradeEntity.getPrice().multiply(tradeEntity.getQuantity());

        if (buyOrder.getOrderType() == OrderType.MARKET) {
            buyWallet.setAvailable(buyWallet.getAvailable().subtract(totalPricePoints));
        }else {
            buyWallet.setLocked(buyWallet.getLocked().subtract(totalPricePoints));
        }
        sellWallet.setAvailable(sellWallet.getAvailable().add(totalPricePoints));

        save(buyWallet);
        save(sellWallet);

        //CRYPTO EXCHANGE

        Wallet SAW = findByUserAndAsset(sellerOrder.getUser(), sellerOrder.getAsset());
        if (sellerOrder.getOrderType() == OrderType.MARKET) {
            SAW.setAvailable(SAW.getAvailable().subtract(tradeEntity.getQuantity()));
        } else {
            SAW.setLocked(SAW.getLocked().subtract(tradeEntity.getQuantity()));
        }
        save(SAW);

        Wallet BAW = findByUserAndAsset(buyOrder.getUser(), buyOrder.getAsset());
        BAW.setAvailable(BAW.getAvailable().add(tradeEntity.getQuantity()));
        save(BAW);
    }

    @Transactional
    public void handleUserOrderCreation(User user, OrderEntity orderEntity, Asset asset) {
        BigDecimal orderPrice = orderEntity.getPrice();
        BigDecimal orderQuantity = orderEntity.getQuantity();

        Wallet wallet;
        if (orderEntity.getSide().equals(Side.BUY)) {
            wallet = getUserDefaultWallet(user);
        } else {
            wallet = findByUserAndAsset(user, asset);
        }

        if (orderEntity.getSide().equals(Side.BUY)) lock(wallet, orderPrice.multiply(orderQuantity));
        else if (orderEntity.getSide().equals(Side.SELL)) lock(wallet, orderQuantity);
        else throw new IllegalArgumentException("Order not locked");
    }

    @Transactional
    public void lock(Wallet wallet, BigDecimal quantity) {
        BigDecimal available = wallet.getAvailable();
        BigDecimal locked = wallet.getLocked();
        if (available.compareTo(quantity) < 0) {
            throw new InsufficientFundsException("Quantity is greater than available credits");
        }
        wallet.setAvailable(available.subtract(quantity));
        wallet.setLocked(locked.add(quantity));
        save(wallet);
    }

    @Transactional
    public void unlock(Wallet wallet, BigDecimal quantity) {
        BigDecimal available = wallet.getAvailable();
        BigDecimal locked = wallet.getLocked();
        if (locked.compareTo(quantity) < 0) {
            throw new IllegalArgumentException("Quantity is greater than locked credits");
        }
        wallet.setAvailable(available.add(quantity));
        wallet.setLocked(locked.subtract(quantity));
        save(wallet);
    }

    public void save(Wallet wallet) {
        walletRepository.save(wallet);
    }

    public Wallet getUserDefaultWallet(User user) {
        Asset asset = assetRepository.findBySymbol("POINTS").orElseThrow(() -> new IllegalStateException("Bse Asset POINTS Not Found"));
        return findByUserAndAsset(user, asset);
    }

    public Wallet findByUserAndAsset(User user, Asset asset) {
        Optional<Wallet> wallet = walletRepository.findByUserAndAsset(user, asset);
        if (wallet.isPresent()) {
            return wallet.get();
        } else {
            walletRepository.insertIfAbsent(user.getId(),  asset.getId());
            return walletRepository.findByUserAndAsset(user, asset).orElseThrow(
                    () -> new IllegalStateException("Wallet not found and failed to create"));
        }
    }
}
