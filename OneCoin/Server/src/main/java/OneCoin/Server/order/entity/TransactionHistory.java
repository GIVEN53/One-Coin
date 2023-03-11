package OneCoin.Server.order.entity;

import OneCoin.Server.audit.CreatedOnlyAuditable;
import OneCoin.Server.coin.entity.Coin;
import OneCoin.Server.order.entity.enums.TransactionType;
import OneCoin.Server.user.entity.User;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionHistory extends CreatedOnlyAuditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long TransactionHistoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private TransactionType transactionType;

    @Column(nullable = false, updatable = false, scale = 15, precision = 30)
    private BigDecimal amount; // 수량

    @Column(nullable = false, updatable = false, scale = 2, precision = 30)
    private BigDecimal price; // 가격

    @Column(nullable = false, updatable = false, scale = 15, precision = 30)
    private BigDecimal totalAmount; // 총 거래 금액

    @Column(nullable = false, updatable = false)
    private double commission;

    @Column(nullable = false, updatable = false, scale = 15, precision = 30)
    private BigDecimal settledAmount; // 정산 금액

    @Column(nullable = false, updatable = false)
    private LocalDateTime orderTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", updatable = false, nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coin_id", updatable = false)
    private Coin coin;

    @Builder
    private TransactionHistory(Long transactionHistoryId, TransactionType transactionType, BigDecimal amount, BigDecimal price, BigDecimal totalAmount, double commission, BigDecimal settledAmount, LocalDateTime orderTime, User user, Coin coin) {
        TransactionHistoryId = transactionHistoryId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.price = price;
        this.totalAmount = totalAmount;
        this.commission = commission;
        this.settledAmount = settledAmount;
        this.orderTime = orderTime;
        this.user = user;
        this.coin = coin;
    }
}