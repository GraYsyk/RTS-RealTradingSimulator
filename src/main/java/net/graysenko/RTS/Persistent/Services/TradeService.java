package net.graysenko.RTS.Persistent.Services;

import lombok.RequiredArgsConstructor;
import net.graysenko.RTS.MatchingEngine.Records.Trade;
import net.graysenko.RTS.Persistent.Entities.Asset;
import net.graysenko.RTS.Persistent.Entities.TradeEntity;
import net.graysenko.RTS.Persistent.Repositories.TradeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TradeService {

    private final TradeRepository tradeRepository;

    public TradeEntity tradify(Trade trade, Asset asset) {
         TradeEntity tradeEntity = new TradeEntity(
                trade.buyOrderId(),
                trade.sellOrderId(),
                trade.price(),
                trade.quantity(),
                asset
        );
         return tradeRepository.save(tradeEntity);
    }
}
