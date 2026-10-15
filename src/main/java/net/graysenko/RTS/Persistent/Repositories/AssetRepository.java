package net.graysenko.RTS.Persistent.Repositories;

import net.graysenko.RTS.Persistent.Entities.Asset;
import net.graysenko.RTS.Persistent.Enums.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    Optional<Asset> findBySymbol(String symbol);
    List<Asset> findAssetsByStatus(AssetStatus status);
}
