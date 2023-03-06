package OneCoin.Server.order.entity;

import OneCoin.Server.order.entity.enums.TransactionType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@RedisHash("order")
public class Order {
    @Id
    private Integer orderId;

    @Indexed
    private BigDecimal limit;

    private BigDecimal market;

    private BigDecimal amount; // 미체결량

    private BigDecimal completedAmount; // 체결량

    private LocalDateTime orderTime;

    @Indexed
    private TransactionType orderType; // ASK, BID

    @Indexed
    private Long userId;

    @Indexed
    private String code;

    @Builder
    private Order(Integer orderId, BigDecimal limit, BigDecimal market, BigDecimal amount, BigDecimal completedAmount, LocalDateTime orderTime, TransactionType orderType, Long userId, String code) {
        this.orderId = orderId;
        this.limit = limit;
        this.market = market;
        this.amount = amount;
        this.completedAmount = completedAmount;
        this.orderTime = orderTime;
        this.orderType = orderType;
        this.userId = userId;
        this.code = code;
    }
}
