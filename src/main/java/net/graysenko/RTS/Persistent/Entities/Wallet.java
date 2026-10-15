package net.graysenko.RTS.Persistent.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "wallets", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "asset_id"}))
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal locked;

    @Column(nullable = false, precision = 30, scale = 10)
    private BigDecimal available;

    public Wallet(User user, Asset asset, BigDecimal available) {
        this.user = user;
        this.asset = asset;
        this.locked = BigDecimal.ZERO;
        this.available = available;
    }
}
