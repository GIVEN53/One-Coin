package OneCoin.Server.order.repository;

import OneCoin.Server.coin.entity.Coin;
import OneCoin.Server.order.entity.TransactionHistory;
import OneCoin.Server.order.entity.enums.TransactionType;
import OneCoin.Server.rank.dao.UserRoi;
import OneCoin.Server.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {
    /**
     * 기간으로 조회
     */
    Page<TransactionHistory> findByUserAndCreatedAtAfter(User user, LocalDateTime searchPeriod, Pageable pageable);

    /**
     * 기간, 거래 타입으로 조회
     */
    Page<TransactionHistory> findByUserAndTransactionTypeAndCreatedAtAfter(User user, TransactionType transactionType, LocalDateTime searchPeriod, Pageable pageable);

    /**
     * 기간, 코인으로 조회
     */
    Page<TransactionHistory> findByUserAndCoinAndCreatedAtAfter(User user, Coin coin, LocalDateTime searchPeriod, Pageable pageable);

    /**
     * 기간, 거래 타입, 코인으로 조회
     */
    Page<TransactionHistory> findByUserAndTransactionTypeAndCoinAndCreatedAtAfter(User user, TransactionType transactionType, Coin coin, LocalDateTime searchPeriod, Pageable pageable);

    List<TransactionHistory> findTop10ByUserAndCoinAndTransactionTypeOrderByCreatedAtDesc(User user, Coin coin, TransactionType transactionType);

    @Query("SELECT " +
            "   new OneCoin.Server.rank.dao.UserRoi(t.user.userId, t.user.displayName, SUM(t.settledAmount))" +
            "FROM " +
            "   TransactionHistory t " +
            "WHERE " +
            "   t.transactionType = 'BID' " +
            "GROUP BY " +
            "   t.user")
    List<UserRoi> findAllSumOfBidSettledAmount();

    @Query("SELECT " +
            "   new OneCoin.Server.rank.dao.UserRoi(t.user.userId, SUM(t.settledAmount))" +
            "FROM " +
            "   TransactionHistory t " +
            "WHERE " +
            "   t.transactionType = 'ASK' " +
            "GROUP BY " +
            "   t.user")
    List<UserRoi> findAllSumOfAskSettledAmount();
}
