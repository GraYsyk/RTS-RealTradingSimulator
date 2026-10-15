package net.graysenko.RTS.Persistent.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.graysenko.RTS.Persistent.Enums.AssetStatus;
import net.graysenko.RTS.Persistent.Enums.CurrencyType;

import java.time.Instant;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "assets")
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true)
    private String symbol;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CurrencyType type;

    private String iconUrl;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AssetStatus status;

    @Column(name = "creation_date", nullable = false)
    private Instant creationDate;

    public Asset(User user, String symbol, String name, CurrencyType type, String iconUrl) {
        this.user = user;
        this.symbol = symbol;
        this.name = name;
        this.type = type;
        this.iconUrl = iconUrl;
        this.status = AssetStatus.PENDING;
        this.creationDate = Instant.now();
    }
}
