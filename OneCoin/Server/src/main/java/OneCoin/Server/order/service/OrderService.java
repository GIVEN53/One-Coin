package OneCoin.Server.order.service;

import OneCoin.Server.balance.BalanceService;
import OneCoin.Server.coin.service.CoinService;
import OneCoin.Server.config.auth.utils.LoggedInUserInfoUtils;
import OneCoin.Server.exception.BusinessLogicException;
import OneCoin.Server.exception.ExceptionCode;
import OneCoin.Server.order.entity.Order;
import OneCoin.Server.order.entity.Wallet;
import OneCoin.Server.order.entity.enums.Commission;
import OneCoin.Server.order.entity.enums.TransactionType;
import OneCoin.Server.order.repository.OrderRepository;
import OneCoin.Server.user.entity.User;
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
    private final LoggedInUserInfoUtils loggedInUserInfoUtils;
    private final BalanceService balanceService;

    public void createOrder(Order order, String code) {
        User user = loggedInUserInfoUtils.extractUser();
        coinService.verifyCoinExists(code);
        long userId = user.getUserId();
        BigDecimal amount = order.getAmount();

        if (order.getOrderType().equals(TransactionType.ASK.getType())) { // 매도
            Wallet wallet = walletService.findVerifiedWalletWithCoin(userId, code);
            checkUserCoinAmount(wallet, amount);
        }
        if (order.getOrderType().equals(TransactionType.BID.getType())) { // 매수
            BigDecimal price = getMyOrderPrice(order);
            subtractUserBalance(userId, price, amount);
            order.setCommission(calculateCommission(price, amount));
        }
        order.setUserId(user.getUserId());
        order.setCode(code);
        orderRepository.save(order);
    }

    private void checkUserCoinAmount(Wallet wallet, BigDecimal amount) {
        BigDecimal myAmount = wallet.getAmount();
        int comparison = myAmount.compareTo(amount);

        if (comparison < 0) {
            throw new BusinessLogicException(ExceptionCode.NOT_ENOUGH_AMOUNT);
        }
    }

    private void subtractUserBalance(long userId, BigDecimal price, BigDecimal amount) {
        BigDecimal totalBidPrice = price.multiply(amount).multiply(Commission.ORDER.getRate());
        balanceService.updateBalanceByBid(userId, totalBidPrice);
    }

    private BigDecimal getMyOrderPrice(Order order) {
        BigDecimal price = null;
        BigDecimal zero = BigDecimal.ZERO;

        if (order.getLimit().compareTo(zero) != 0) {
            price = order.getLimit();
        } else if (order.getMarket().compareTo(zero) != 0) {
            price = order.getMarket();
        }
        return price;
    }

    private BigDecimal calculateCommission(BigDecimal price, BigDecimal amount) {
        BigDecimal commissionRate = Commission.ORDER.getRate().subtract(BigDecimal.ONE); // 0.05
        return price.multiply(amount).multiply(commissionRate);
    }

    public void cancelOrder(long orderId) {
        Order order = findVerifiedOrder(orderId);
        long userId = verifyUserOrder(order);

        if (order.getOrderType().equals(TransactionType.BID.getType())) { // 매수 주문 취소 시 balance 환불
            giveBalanceBack(userId, order);
        }
        orderRepository.delete(order);
    }

    private Order findVerifiedOrder(long orderId) {
        Optional<Order> optionalRedisOrder = orderRepository.findById(orderId);
        return optionalRedisOrder.orElseThrow(() -> new BusinessLogicException(ExceptionCode.NO_EXISTS_ORDER));
    }

    private long verifyUserOrder(Order order) {
        User user = loggedInUserInfoUtils.extractUser();
        long userId = user.getUserId();
        if (order.getUserId() != userId) {
            throw new BusinessLogicException(ExceptionCode.NOT_YOUR_ORDER);
        }
        return userId;
    }

    private void giveBalanceBack(long userId, Order order) {
        BigDecimal cancelPrice = getMyOrderPrice(order);
        BigDecimal totalCancelPrice = cancelPrice.multiply(order.getAmount()).multiply(Commission.ORDER.getRate());
        balanceService.updateBalanceByAskOrCancelBid(userId, totalCancelPrice);
    }

    @Transactional(readOnly = true)
    public List<Order> findOrders(String code) {
        long userId = loggedInUserInfoUtils.extractUserId();
        List<Order> myOrders = orderRepository.findAllByUserIdAndCode(userId, code);
        if (myOrders.isEmpty()) {
            throw new BusinessLogicException(ExceptionCode.NO_EXISTS_ORDER);
        }
        return myOrders;
    }
}
