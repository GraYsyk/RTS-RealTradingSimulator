package net.graysenko.RTS.MatchingEngine.POJO;

import java.util.ArrayDeque;

public class PriceLevel {

    private final ArrayDeque<Order> orders = new ArrayDeque<>();
//    private BigDecimal totalVolume = BigDecimal.ZERO;

    public void addOrder(Order order) {
        if (order != null) {
            orders.addLast(order);
        } else {
            throw new  IllegalArgumentException("Order is empty, order info: ");
        }
    }

    public Order peekOrder() {
        return orders.peek();
    }

    public Order pollOrder() {
        return orders.poll();
    }

    public boolean isEmpty() {
        return orders.isEmpty();
    }

    public int size() {
        return orders.size();
    }

}
