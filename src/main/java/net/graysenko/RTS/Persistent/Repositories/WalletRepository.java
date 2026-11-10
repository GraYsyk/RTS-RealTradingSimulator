package net.graysenko.RTS.Persistent.Repositories;

import jakarta.persistence.LockModeType;
import net.graysenko.RTS.Persistent.Entities.Asset;
import net.graysenko.RTS.Persistent.Entities.User;
import net.graysenko.RTS.Persistent.Entities.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Wallet> findByUserAndAsset(User user, Asset asset);

    @Modifying
    @Query(value = """
        INSERT INTO wallets (user_id, asset_id, available, locked)
        VALUES (:userId, :assetId, 0, 0)
        ON CONFLICT (user_id, asset_id) DO NOTHING
        """, nativeQuery = true)
    void insertIfAbsent(@Param("userId") Long userId, @Param("assetId") Long assetId);
}
