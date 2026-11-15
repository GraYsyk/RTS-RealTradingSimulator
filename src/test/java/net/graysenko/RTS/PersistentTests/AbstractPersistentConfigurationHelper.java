package net.graysenko.RTS.PersistentTests;

import net.graysenko.RTS.Persistent.Entities.Asset;
import net.graysenko.RTS.Persistent.Entities.OrderEntity;
import net.graysenko.RTS.Persistent.Entities.User;
import net.graysenko.RTS.Persistent.Entities.Wallet;
import net.graysenko.RTS.Persistent.Enums.AssetStatus;
import net.graysenko.RTS.Persistent.Enums.CurrencyType;
import net.graysenko.RTS.Persistent.Repositories.AssetRepository;
import net.graysenko.RTS.Persistent.Repositories.OrderRepository;
import net.graysenko.RTS.Persistent.Repositories.UserRepository;
import net.graysenko.RTS.Persistent.Repositories.WalletRepository;
import net.graysenko.RTS.Persistent.Services.AssetService;
import net.graysenko.RTS.Persistent.Services.OrderService;
import net.graysenko.RTS.Persistent.Services.UserService;
import net.graysenko.RTS.Persistent.Services.WalletService;
import net.graysenko.RTS.Persistent.Utils.MatchingEngineRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootTest
@Import(PostgreTestConfig.class)
public abstract class AbstractPersistentConfigurationHelper {

    @Autowired
    protected OrderRepository orderRepository;
    @Autowired
    protected OrderService orderService;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected UserService userService;
    @Autowired
    protected WalletRepository walletRepository;
    @Autowired
    protected WalletService walletService;
    @Autowired
    protected AssetRepository assetRepository;
    @Autowired
    protected AssetService assetService;
    @Autowired
    protected MatchingEngineRegistry matchingEngineRegistry;

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length();

    private static final AtomicLong COUNTER = new AtomicLong(1);

    protected Asset pointsAsset;

    @BeforeEach
    public void setup() {
        pointsAsset = assetService.findAssetBySymbol("POINTS");
    }

    //Base62 encoding
    private String getUniqueString() {
        long number = COUNTER.getAndIncrement();

        StringBuilder sb = new StringBuilder();
        while (number > 0) {
            int remainder = (int) (number % BASE);
            sb.append(ALPHABET.charAt(remainder));
            number /= BASE;
        }
        return "test-" + sb.reverse().toString();
    }

    protected User createUserWithAsset(BigDecimal amount, Asset asset) {
        User user = new User(getUniqueString(), getUniqueString()); //email & passwordHash
        userRepository.save(user);

        Wallet wallet = walletService.findByUserAndAsset(user, asset);
        wallet.setAvailable(amount);

        walletRepository.save(wallet);

        return user;
    }

    protected User createUserWithPoints(BigDecimal amount) {
        return createUserWithAsset(amount, pointsAsset);
    }

    protected Asset setupTestingAsset(){
        Asset asset = new Asset(
                null,
                getUniqueString(),
                getUniqueString(),
                CurrencyType.CRYPTO,
                null
        );
        asset.setStatus(AssetStatus.APPROVED);
        assetRepository.save(asset);
        matchingEngineRegistry.rebuildEngine(asset.getId());

        return asset;
    }

    protected OrderEntity fetchFreshOrderEntity(OrderEntity orderEntity) {
        return orderRepository.findById(orderEntity.getId())
                .orElseThrow(() -> new IllegalStateException("Order was not found!"));
    }

    protected Wallet findWalletByUserAndAsset(User user, Asset asset) {
        Optional<Wallet> optionalWallet = walletRepository.readByUserAndAsset(user, asset);
        return optionalWallet.orElseThrow(() -> new IllegalStateException("Wallet not found"));
    }
}
