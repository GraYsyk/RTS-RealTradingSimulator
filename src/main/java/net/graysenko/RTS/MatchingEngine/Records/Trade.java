package net.graysenko.RTS.MatchingEngine.Records;

import java.math.BigDecimal;
import java.time.Instant;

public record Trade(long id, long buyOrderId, long sellOrderId,
                    BigDecimal price, BigDecimal quantity, Instant timestamp) {
}
