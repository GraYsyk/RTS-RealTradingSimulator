package net.graysenko.RTS.Persistent.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "trades")
public class TradeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "buy_order_id")
    private long buyOrderId;

    @Column(nullable = false, name = "sell_order_id")
    private long sellOrderId;

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal price; //POINTS ONLY

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal quantity; //ASSET ONLY

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    public TradeEntity(long buyOrderId, long sellOrderId, BigDecimal price, BigDecimal quantity, Asset asset) {
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.price = price;
        this.quantity = quantity;
        this.asset = asset;
        this.executedAt = Instant.now();
    }
}
