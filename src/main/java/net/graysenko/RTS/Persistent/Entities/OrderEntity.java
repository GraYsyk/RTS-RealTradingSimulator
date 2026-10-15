package net.graysenko.RTS.Persistent.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.graysenko.RTS.MatchingEngine.Enums.OrderType;
import net.graysenko.RTS.MatchingEngine.Enums.Side;
import net.graysenko.RTS.MatchingEngine.Enums.Status;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Side side;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;

    @Setter
    @Column(precision = 30, scale = 10)
    private BigDecimal price;

    @Setter
    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal quantity;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal remainingQuantity;

    @Setter
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    public OrderEntity(User user, Side side,
                       OrderType orderType, BigDecimal price,
                       BigDecimal quantity, Asset asset,
                       BigDecimal remainingQuantity, Status status) {
        this.user = user;
        this.side = side;
        this.orderType = orderType;
        this.price = price;
        this.quantity = quantity;
        this.asset = asset;
        this.remainingQuantity = remainingQuantity;
        this.createdAt = Instant.now();
        this.status = status;
    }

    public void fill(BigDecimal amount) {
        if (amount.compareTo(remainingQuantity) <= 0) {
            remainingQuantity = remainingQuantity.subtract(amount);
            updateStatus();
        }
        else {
            throw new IllegalArgumentException(
                    "Fill amount " + amount + " exceeds remaining quantity " + remainingQuantity);
        }
    }

    private void updateStatus() {
        if (remainingQuantity.signum() == 0) {
            this.status = Status.FILLED;
        } else if(remainingQuantity.compareTo(quantity) < 0) {
            this.status = Status.PARTIALLY_FILLED;
        } else {
            this.status = Status.OPEN;
        }
    }

    public void cancel() {
        this.status = Status.CANCELLED;
    }
}
