package OneCoin.Server.order.service;

import OneCoin.Server.coin.entity.Coin;
import OneCoin.Server.coin.service.CoinService;
import OneCoin.Server.deposit.entity.Deposit;
import OneCoin.Server.exception.BusinessLogicException;
import OneCoin.Server.exception.ExceptionCode;
import OneCoin.Server.order.entity.Order;
import OneCoin.Server.order.entity.TransactionHistory;
import OneCoin.Server.order.entity.enums.Period;
import OneCoin.Server.order.entity.enums.TransactionType;
import OneCoin.Server.order.mapper.TransactionHistoryMapper;
import OneCoin.Server.order.repository.TransactionHistoryRepository;
import OneCoin.Server.swap.entity.Swap;
import OneCoin.Server.user.entity.User;
import OneCoin.Server.user.service.UserService;
import OneCoin.Server.utils.CalculationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TransactionHistoryService {
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final TransactionHistoryMapper mapper;
    private final UserService userService;
    private final CoinService coinService;
    private final CalculationUtil calculationUtil;
    private final String DEFAULT_TRANSACTION_TYPE = "ALL";

    /**
     * 주문 체결 시 투자 내역을 생성한다.
     */
    @Async("upbitExecutor")
    public void createTransactionHistoryByOrder(Order order) {
        User user = userService.findVerifiedUser(order.getUserId());
        Coin coin = coinService.findCoin(order.getCode());
        BigDecimal price = order.getLimit();
        BigDecimal amount = order.getAmount();

        BigDecimal commission = calculationUtil.calculateOrderCommission(price, amount);
        BigDecimal totalAmount = price.multiply(amount);
        BigDecimal settledAmount = getSettledAmount(order.getOrderType(), totalAmount, commission);

        TransactionHistory transactionHistory = mapper.orderToTransactionHistory(order, user, coin, totalAmount, commission.doubleValue(), settledAmount);
        transactionHistoryRepository.save(transactionHistory);
    }

    /**
     * 매수(BID) 체결 시 최종 금액: 체결 금액 + 수수료
     * 매도(ASK) 체결 시 최종 금액: 체결 금액 - 수수료
     */
    private BigDecimal getSettledAmount(TransactionType transactionType, BigDecimal totalAmount, BigDecimal commission) {
        if (transactionType.equals(TransactionType.BID)) {
            return totalAmount.add(commission);
        } else {
            return totalAmount.subtract(commission);
        }
    }

    /**
     * 스왑 시 투자 내역을 생성한다.
     */
    public void createTransactionHistoryBySwap(Swap swap) {
        BigDecimal totalAmount = swap.getGivenAmount().multiply(swap.getGivenCoinPrice());
        BigDecimal settledAmount = swap.getGivenAmount().subtract(swap.getCommission());
        TransactionHistory transactionHistory = mapper.swapToTransactionHistory(swap, totalAmount, settledAmount);

        transactionHistoryRepository.save(transactionHistory);
    }

    /**
     * 입금 시 투자 내역을 생성한다.
     */
    public void createTransactionHistoryByDeposit(Deposit deposit) {
        TransactionHistory transactionHistory = mapper.depositToTransactionHistory(deposit);

        transactionHistoryRepository.save(transactionHistory);
    }

    /**
     * 파라미터 존재 여부에 따라 pagination을 처리한다.
     * 기간은 week를 기본값으로 설정.
     * @param period 기간
     * @param type 거래 타입
     * @param code 코인 코드
     * @param pageRequest 페이지 정보
     * @param userId 사용자 아이디
     */
    @Transactional(readOnly = true)
    public Page<TransactionHistory> findTransactionHistory(String period, String type, String code, PageRequest pageRequest, long userId) {
        User user = userService.findVerifiedUser(userId);
        LocalDateTime searchPeriod = getSearchPeriod(period);

        if (code != null) {
            Coin coin = coinService.findCoin(code);
            return findWithCode(coin, user, searchPeriod, pageRequest, type);
        } else {
            return findWithoutCode(user, searchPeriod, pageRequest, type);
        }
    }

    /**
     * 코인으로 조회할 경우
     * 거래 타입이 기본값이면 모든 거래 타입으로 조회
     * 거래 타입을 지정하면 해당 거래 타입으로 조회
     */
    private Page<TransactionHistory> findWithCode(Coin coin, User user, LocalDateTime searchPeriod, PageRequest pageRequest, String type) {
        if (type.equals(DEFAULT_TRANSACTION_TYPE)) {
            return transactionHistoryRepository.findByUserAndCoinAndCreatedAtAfter(user, coin, searchPeriod, pageRequest);
        } else {
            TransactionType transactionType = getTransactionType(type);
            return transactionHistoryRepository.findByUserAndTransactionTypeAndCoinAndCreatedAtAfter(user, transactionType, coin, searchPeriod, pageRequest);
        }
    }

    /**
     * 코인으로 조회하지 않을 경우
     * 거래 타입이 기본값이면 모든 거래 타입으로 조회
     * 거래 타입을 지정하면 해당 거래 타입으로 조회
     */
    private Page<TransactionHistory> findWithoutCode(User user, LocalDateTime searchPeriod, PageRequest pageRequest, String type) {
        if (type.equals(DEFAULT_TRANSACTION_TYPE)) {
            return transactionHistoryRepository.findByUserAndCreatedAtAfter(user, searchPeriod, pageRequest);
        } else {
            TransactionType transactionType = getTransactionType(type);
            return transactionHistoryRepository.findByUserAndTransactionTypeAndCreatedAtAfter(user, transactionType, searchPeriod, pageRequest);
        }
    }

    /**
     * 현재 시간을 기준으로 period 이전의 날짜를 리턴한다.
     * @throws BusinessLogicException 올바르지 않은 Period일 경우
     * @return searchPeriod
     */
    private LocalDateTime getSearchPeriod(String period) {
        LocalDateTime searchPeriod = LocalDateTime.now();

        if (period.equals(Period.WEEK.getAbbreviation())) {
            return searchPeriod.minusWeeks(1);
        } else if (period.equals(Period.MONTH.getAbbreviation())) {
            return searchPeriod.minusMonths(1);
        } else if (period.equals(Period.THREE_MONTHS.getAbbreviation())) {
            return searchPeriod.minusMonths(3);
        } else if (period.equals(Period.SIX_MONTHS.getAbbreviation())) {
            return searchPeriod.minusMonths(6);
        } else {
            throw new BusinessLogicException(ExceptionCode.NOT_CORRECT_PERIOD);
        }
    }

    /**
     * @throws BusinessLogicException 올바르지 않은 TransactionType일 경우
     * @return TransactionType
     */
    private TransactionType getTransactionType(String type) {
        try {
            return Enum.valueOf(TransactionType.class, type);
        } catch (IllegalArgumentException e) {
            throw new BusinessLogicException(ExceptionCode.NOT_CORRECT_TYPE);
        }
    }

    /**
     * 최근 매수/매도 투자 내역 10개를 조회한다.
     */
    public List<TransactionHistory> findOrderTransactionHistory(String code, long userId) {
        User user = userService.findVerifiedUser(userId);
        Coin coin = coinService.findCoin(code);

        return getTop10OrderTransactionHistories(user, coin);
    }

    /**
     * 매수(BID)와 매도(ASK)의 최근 투자 내역을 각각 10개씩 조회한 후 최근 순으로 정렬하여 상위 10개만 리턴한다.
     */
    private List<TransactionHistory> getTop10OrderTransactionHistories(User user, Coin coin) {
        List<TransactionHistory> transactionHistories = transactionHistoryRepository.findTop10ByUserAndCoinAndTransactionTypeOrderByCreatedAtDesc(user, coin, TransactionType.BID);
        transactionHistories.addAll(transactionHistoryRepository.findTop10ByUserAndCoinAndTransactionTypeOrderByCreatedAtDesc(user, coin, TransactionType.ASK));
        transactionHistories.sort(Comparator.comparing(TransactionHistory::getCreatedAt).reversed());
        if (transactionHistories.size() > 10) {
            return transactionHistories.subList(0, 9);
        }
        return transactionHistories;
    }
}
