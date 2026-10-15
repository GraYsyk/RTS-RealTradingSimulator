package net.graysenko.RTS.Persistent.DTO;

import net.graysenko.RTS.MatchingEngine.Enums.OrderType;
import net.graysenko.RTS.MatchingEngine.Enums.Side;

import java.math.BigDecimal;

public record OrderPlacementRequest(
   Long userId,
   String assetSymbol,
   Side side,
   OrderType orderType,
   BigDecimal price,
   BigDecimal quantity
   ) {}
