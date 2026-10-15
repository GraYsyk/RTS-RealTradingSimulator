package net.graysenko.RTS.Persistent.Services;

import lombok.RequiredArgsConstructor;
import net.graysenko.RTS.Persistent.Entities.Asset;
import net.graysenko.RTS.Persistent.Repositories.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AssetService {
    private final AssetRepository assetRepository;

    public Asset findById(Long id) {
        return assetRepository.findById(id).orElse(null);
    }

    public Asset findAssetBySymbol(String symbol) {
        return assetRepository.findBySymbol(symbol).orElse(null);
    }
}
