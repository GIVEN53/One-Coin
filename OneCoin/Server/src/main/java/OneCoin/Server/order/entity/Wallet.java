package OneCoin.Server.order.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.math.BigDecimal;

@Getter
@Setter
@RedisHash("wallet")
public class Wallet {
    @Id
    private Long walletId;

    private BigDecimal amount; // 보유 수량

    private BigDecimal averagePrice; // 평단가

    @Indexed
    private Long userId;

    @Indexed
    private String code;

    @Builder
    private Wallet(Long walletId, BigDecimal amount, BigDecimal averagePrice, Long userId, String code) {
        this.walletId = walletId;
        this.amount = amount;
        this.averagePrice = averagePrice;
        this.userId = userId;
        this.code = code;
    }
}
