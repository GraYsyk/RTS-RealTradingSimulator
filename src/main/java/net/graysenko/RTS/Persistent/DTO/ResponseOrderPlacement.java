package net.graysenko.RTS.Persistent.DTO;

import net.graysenko.RTS.MatchingEngine.Enums.Status;

import java.math.BigDecimal;

public record ResponseOrderPlacement(
        Long orderId,
        Status status,
        BigDecimal remainingQuantity,
        BigDecimal quantity,
        BigDecimal price
        ){}
