package net.graysenko.RTS.MatchingEngine.POJO;

import lombok.Getter;
import net.graysenko.RTS.MatchingEngine.Enums.OrderType;
import net.graysenko.RTS.MatchingEngine.Enums.Side;
import net.graysenko.RTS.MatchingEngine.Enums.Status;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
public class Order {

    private long id;
    private Long userId;
    private Side side;
    private OrderType orderType;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal remainingQuantity;
    private Instant timestamp;

    private Status status;

    public Order(long id, Long userId, Side side, OrderType orderType,
                 BigDecimal price, BigDecimal quantity,
                 Instant timestamp) {
        this.id = id;
        this.userId = userId;
        this.side = side;
        this.orderType = orderType;
        this.price = price;
        this.quantity = quantity;
        this.remainingQuantity = quantity;
        this.timestamp = timestamp;
        this.status = Status.OPEN;
    }

    public Order(long id, Long userId, Side side, OrderType orderType,
                 BigDecimal price, BigDecimal quantity, BigDecimal remainingQuantity,
                 Instant timestamp, Status status) {
        this.id = id;
        this.userId = userId;
        this.side = side;
        this.orderType = orderType;
        this.price = price;
        this.quantity = quantity;
        this.remainingQuantity = remainingQuantity;
        this.timestamp = timestamp;
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

    public void updateStatus() {
        if (remainingQuantity.signum() == 0) {
            status = Status.FILLED;
        } else if(remainingQuantity.compareTo(quantity) < 0) {
            status = Status.PARTIALLY_FILLED;
        } else {
            status = Status.OPEN;
        }
    }

    public void cancel() {
        this.status = Status.CANCELLED;
    }

    public boolean isFilled() {
        return remainingQuantity.signum() == 0;
    }
}
