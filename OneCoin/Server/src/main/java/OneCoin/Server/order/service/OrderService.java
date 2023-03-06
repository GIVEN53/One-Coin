package OneCoin.Server.order.service;

import OneCoin.Server.balance.service.BalanceService;
import OneCoin.Server.coin.service.CoinService;
import OneCoin.Server.exception.BusinessLogicException;
import OneCoin.Server.exception.ExceptionCode;
import OneCoin.Server.order.entity.Order;
import OneCoin.Server.order.entity.enums.TransactionType;
import OneCoin.Server.order.repository.OrderRepository;
import OneCoin.Server.utils.CalculationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final CoinService coinService;
    private final WalletService walletService;
    private final CalculationUtil calculationUtil;
    private final BalanceService balanceService;
    private final TransactionHistoryService transactionHistoryService;

    /**
     * 주문을 생성한다.
     * 매도(ASK) 주문: 사용자의 지갑에서 코인 보유량을 확인한다.
     * 매수(BID) 주문: 사용자의 잔액에서 주문 금액만큼 선차감한다.
     * @param order 주문
     * @param userId 사용자 아이디
     * @param code 코인 코드
     */
    public void createOrder(Order order, long userId, String code) {
        coinService.findCoin(code);

        if (order.getOrderType().equals(TransactionType.ASK)) {
            walletService.verifyWalletAmount(userId, code, order.getAmount());
        }
        if (order.getOrderType().equals(TransactionType.BID)) {
            subtractUserBalance(userId, order.getLimit(), order.getAmount());
        }
        order.setUserId(userId);
        order.setCode(code);
        orderRepository.save(order);
    }

    /**
     * 미체결된 매도 코인량을 가져온다.
     */
    protected BigDecimal getPrevAskOrderAmount(long userId, String code) {
        List<Order> prevAskOrders = orderRepository.findAllByUserIdAndOrderTypeAndCode(userId, TransactionType.ASK, code);
        BigDecimal amount = BigDecimal.ZERO;
        for (Order order : prevAskOrders) {
            amount = amount.add(order.getAmount());
        }
        return amount;
    }

    /**
     * 사용자의 잔액에서 매수 금액 + 수수료만큼 차감한다.
     */
    private void subtractUserBalance(long userId, BigDecimal price, BigDecimal amount) {
        BigDecimal totalBidPrice = calculationUtil.calculateByAddingCommission(price, amount);
        balanceService.updateBalanceByBid(userId, totalBidPrice);
    }

    /**
     * 미체결 주문 내역을 조회한다.
     * @throws BusinessLogicException 주문 내역이 없을 경우
     * @return myOrders
     */
    @Transactional(readOnly = true)
    public List<Order> findOrders(long userId) {
        List<Order> myOrders = orderRepository.findAllByUserId(userId);

        if (myOrders.isEmpty()) {
            throw new BusinessLogicException(ExceptionCode.NO_EXISTS_ORDER);
        }
        return myOrders;
    }

    /**
     * 주문을 취소한다.
     * @param orderId 주문 아이디
     * @param userId 사용자 아이디
     */
    public void cancelOrder(long orderId, long userId) {
        Order order = findVerifiedOrder(orderId);
        verifyUserOrder(order, userId);

        if (order.getOrderType().equals(TransactionType.BID)) {
            giveBalanceBack(userId, order.getLimit(), order.getAmount());
        }
        savePartialTradedOrdersToTransactionHistory(order);
        orderRepository.delete(order);
    }

    /**
     * 아이디로 주문을 조회한다.
     * @throws BusinessLogicException 주문 내역이 없을 경우
     */
    private Order findVerifiedOrder(long orderId) {
        Optional<Order> optionalRedisOrder = orderRepository.findById(orderId);
        return optionalRedisOrder.orElseThrow(() -> new BusinessLogicException(ExceptionCode.NO_EXISTS_ORDER));
    }

    /**
     * 주문을 취소하려는 사용자가 해당 주문의 소유자인지 확인한다.
     * @throws BusinessLogicException 주문에 등록된 사용자와 주문을 취소하는 사용자가 다를 경우
     */
    private void verifyUserOrder(Order order, long userId) {
        if (order.getUserId() != userId) {
            throw new BusinessLogicException(ExceptionCode.NOT_YOUR_ORDER);
        }
    }

    /**
     * 매수 주문 취소의 경우 미리 수취한 금액을 환불한다.
     */
    private void giveBalanceBack(long userId, BigDecimal cancelPrice, BigDecimal cancelAmount) {
        BigDecimal totalCancelPrice = calculationUtil.calculateByAddingCommission(cancelPrice, cancelAmount);
        balanceService.updateBalanceByAskOrCancelBid(userId, totalCancelPrice);
    }

    /**
     * 일부 체결된 주문량이 있을 경우 투자 내역에 저장한다.
     */
    private void savePartialTradedOrdersToTransactionHistory(Order order) {
        int comparison = order.getCompletedAmount().compareTo(BigDecimal.ZERO);
        if (comparison > 0) {
            transactionHistoryService.createTransactionHistoryByOrder(order);
        }
    }
}
