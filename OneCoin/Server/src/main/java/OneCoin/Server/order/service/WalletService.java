package OneCoin.Server.order.service;

import OneCoin.Server.balance.service.BalanceService;
import OneCoin.Server.exception.BusinessLogicException;
import OneCoin.Server.exception.ExceptionCode;
import OneCoin.Server.order.entity.Order;
import OneCoin.Server.order.entity.Wallet;
import OneCoin.Server.order.entity.enums.TransactionType;
import OneCoin.Server.order.mapper.WalletMapper;
import OneCoin.Server.order.repository.OrderRepository;
import OneCoin.Server.order.repository.WalletRepository;
import OneCoin.Server.utils.CalculationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {
    private final WalletRepository walletRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final WalletMapper mapper;
    private final BalanceService balanceService;
    private final TransactionHistoryService transactionHistoryService;
    private final CalculationUtil calculationUtil;

    /**
     * 사용자의 지갑을 조회한다.
     * 없으면 null을 리턴하여 wallet을 새로 생성하도록 한다.
     * @param userId 사용자 아이디
     * @param code 코인 코드
     * @return wallet
     */
    public Wallet findWallet(long userId, String code) {
        return walletRepository.findByUserIdAndCode(userId, code).orElse(null);
    }

    /**
     * 매수(BID) 주문 체결 시 지갑을 생성한다.
     * 지갑이 이미 존재할 경우 지갑을 업데이트한다.
     * @param order 체결된 주문
     * @param tradePrice 체결 가격
     * @param tradeVolume 체결량
     */
    public void updateWalletByBid(Order order, BigDecimal tradePrice, BigDecimal tradeVolume) {
        BigDecimal completedAmount = getCompletedAmount(order, tradeVolume);
        BigDecimal completedPrice = getCompletedPrice(order.getOrderType(), order.getLimit(), tradePrice);

        Wallet findWallet = findWallet(order.getUserId(), order.getCode());
        Wallet wallet;
        if (findWallet == null) {
            wallet = mapper.bidOrderToNewWallet(order, completedPrice, completedAmount);
        } else {
            wallet = mapper.bidOrderToUpdatedWallet(findWallet, completedPrice, completedAmount);
        }

        walletRepository.save(wallet);
        createTransactionHistory(completedAmount, completedPrice, order);
    }

    /**
     * 매도(ASK) 주문 체결 시 지갑을 업데이트한다.
     * 지갑에서 해당 코인의 보유량이 0이 될 경우 지갑을 삭제한다.
     * 매도 금액에서 수수료를 뺀 만큼 사용자의 잔액을 증액한다.
     * @param order 체결된 주문
     * @param tradePrice 체결 가격
     * @param tradeVolume 체결량
     */
    public void updateWalletByAsk(Order order, BigDecimal tradePrice, BigDecimal tradeVolume) {
        BigDecimal completedAmount = getCompletedAmount(order, tradeVolume);
        BigDecimal completedPrice = getCompletedPrice(order.getOrderType(), order.getLimit(), tradePrice);

        Wallet findWallet = findWallet(order.getUserId(), order.getCode());
        Wallet updatedWallet = mapper.askOrderToUpdatedWallet(findWallet, completedAmount);
        if (verifyWalletAmountZero(updatedWallet)) {
            walletRepository.delete(updatedWallet);
        } else {
            walletRepository.save(updatedWallet);
        }

        BigDecimal totalAskPrice = calculationUtil.calculateBySubtractingCommission(completedPrice, completedAmount);
        balanceService.updateBalanceByAskOrCancelBid(order.getUserId(), totalAskPrice);
        createTransactionHistory(completedAmount, completedPrice, order);
    }

    /**
     * 미체결량이 체결량보다 작거나 같을 경우 주문을 삭제한다.
     * 미체결량이 체결량보다 클 경우 주문의 미체결량을 업데이트한다.
     */
    private BigDecimal getCompletedAmount(Order order, BigDecimal tradeVolume) {
        BigDecimal orderAmount = order.getAmount();

        int comparison = orderAmount.compareTo(tradeVolume);
        if (comparison <= 0) {
            orderRepository.delete(order);
            return orderAmount;
        }
        order.setAmount(order.getAmount().subtract(tradeVolume));
        orderRepository.save(order);
        return tradeVolume;
    }

    /**
     * 매수(BID) 주문: 주문 가격이 체결 가격보다 클 경우 체결 가격을 리턴한다.
     * 매도(ASK) 주문: 주문 가격이 체결 가격보다 작을 경우 체결 가격을 리턴한다.
     */
    private BigDecimal getCompletedPrice(TransactionType transactionType, BigDecimal orderPrice, BigDecimal tradePrice) {
        int comparison = orderPrice.compareTo(tradePrice);

        if (transactionType.equals(TransactionType.BID)) {
            if (comparison > 0) {
                return tradePrice;
            } else {
                return orderPrice;
            }
        } else {
            if (comparison < 0) {
                return tradePrice;
            } else {
                return orderPrice;
            }
        }
    }

    /**
     * 지갑에서 해당 코인의 보유량이 0인지 확인한다.
     */
    private boolean verifyWalletAmountZero(Wallet wallet) {
        BigDecimal amount = wallet.getAmount();
        int comparison = amount.compareTo(BigDecimal.ZERO);
        if (comparison < 0) {
            log.error(ExceptionCode.OCCURRED_NEGATIVE_AMOUNT.getDescription());
        }
        return comparison == 0;
    }

    /**
     * 투자 내역을 생성한다.
     */
    private void createTransactionHistory(BigDecimal completedAmount, BigDecimal completedPrice, Order order) {
        order.setAmount(completedAmount);
        order.setLimit(completedPrice);
        transactionHistoryService.createTransactionHistoryByOrder(order);
    }

    /**
     * 매도 주문 또는 스왑 시 (보유 코인량 - 아직 미체결된 매도 코인량)으로 해당 주문을 이행할 수 있는지 확인한다.
     */
    public void verifyWalletAmount(Wallet myWallet, BigDecimal orderAmount) {
        BigDecimal myWalletAmount = myWallet.getAmount();
        BigDecimal prevAskOrderAmount = orderService.getPrevAskOrderAmount(myWallet.getUserId(), myWallet.getCode());

        BigDecimal sellableAmount = myWalletAmount.subtract(prevAskOrderAmount);
        if (sellableAmount.compareTo(orderAmount) < 0) {
            throw new BusinessLogicException(ExceptionCode.NOT_ENOUGH_AMOUNT);
        }
    }

    /**
     * 사용자의 유효한 지갑을 조회한다.
     * @throws BusinessLogicException 지갑이 존재하지 않을 경우
     * @return Wallet
     */
    public Wallet findMyVerifiedWallet(long userId, String code) {
        return walletRepository.findByUserIdAndCode(userId, code).orElseThrow(() -> new BusinessLogicException(ExceptionCode.HAVE_NO_COIN));
    }

    /**
     * 사용자가 보유한 모든 지갑을 조회한다.
     * @param userId 사용자 아이디
     * @return wallets
     */
    public List<Wallet> findWallets(long userId) {
        List<Wallet> wallets = walletRepository.findAllByUserId(userId);
        if (wallets.isEmpty()) {
            throw new BusinessLogicException(ExceptionCode.NO_EXISTS_WALLET);
        }
        return wallets;
    }

    public void createWalletByTakenSwap(Wallet takenWallet) {
        walletRepository.save(takenWallet);
    }

    public void updateWalletByGivenSwap(Wallet wallet, Wallet givenWallet) {
        BigDecimal amount = wallet.getAmount().subtract(givenWallet.getAmount());
        wallet.setAmount(amount);

        if (verifyWalletAmountZero(wallet)) {
            walletRepository.delete(wallet);
        } else {
            walletRepository.save(wallet);
        }
    }

    public void updateWalletByTakenSwap(Wallet wallet, Wallet takenWallet) {
        BigDecimal amount = wallet.getAmount().add(takenWallet.getAmount());
        BigDecimal averagePrice = calculationUtil.calculateAvgPrice(wallet.getAveragePrice(), wallet.getAmount(), takenWallet.getAveragePrice(), takenWallet.getAmount());

        wallet.setAmount(amount);
        wallet.setAveragePrice(averagePrice);

        walletRepository.save(wallet);
    }
}
