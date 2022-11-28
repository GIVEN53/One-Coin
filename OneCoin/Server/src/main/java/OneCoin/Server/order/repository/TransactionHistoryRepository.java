package OneCoin.Server.order.repository;

import OneCoin.Server.coin.entity.Coin;
import OneCoin.Server.order.entity.TransactionHistory;
import OneCoin.Server.order.entity.enums.TransactionType;
import OneCoin.Server.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {

    Page<TransactionHistory> findByUserAndCreatedAtAfter(User user, LocalDateTime searchPeriod, Pageable pageable); // 기간 page
    Page<TransactionHistory> findByUserAndTransactionTypeAndCreatedAtAfter(User user, TransactionType transactionType, LocalDateTime searchPeriod, Pageable pageable); // 기간, 타입 page

    Page<TransactionHistory> findByUserAndCoinAndCreatedAtAfter(User user, Coin coin, LocalDateTime searchPeriod, Pageable pageable); // 기간, 코인 page
    Page<TransactionHistory> findByUserAndTransactionTypeAndCoinAndCreatedAtAfter(User user, TransactionType transactionType, Coin coin, LocalDateTime searchPeriod, Pageable pageable); // 기간, 타입, 코인 page

}
