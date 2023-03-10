package OneCoin.Server.upbit.service;

import OneCoin.Server.order.entity.Order;
import OneCoin.Server.order.entity.enums.TransactionType;
import OneCoin.Server.order.repository.OrderRepository;
import OneCoin.Server.order.service.WalletService;
import OneCoin.Server.upbit.entity.Trade;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TradingService {
    private final OrderRepository orderRepository;
    private final WalletService walletService;

    /**
     * upbit에서 전달받은 체결 데이터와 같은 거래 타입, 코인을 order repository에서 조회한 후 체결시킨다.
     */
    @EventListener
    public void completeOrders(Trade trade) {
        BigDecimal tradePrice = new BigDecimal(trade.getTradePrice());
        BigDecimal tradeVolume = new BigDecimal(trade.getTradeVolume());
        TransactionType orderType = Enum.valueOf(TransactionType.class, trade.getOrderType());

        List<Order> orders = orderRepository.findAllByOrderTypeAndCode(orderType, trade.getCode());
        if (orders.isEmpty()) {
            return;
        }

        if (orderType.equals(TransactionType.BID)) {
            tradeBid(orders, tradePrice, tradeVolume);
        } else {
            tradeAsk(orders, tradePrice, tradeVolume);
        }
    }

    /**
     * 매수(BID): 현재 체결 가격보다 작은 주문은 체결시키지 않는다.
     */
    private void tradeBid(List<Order> orders, BigDecimal tradePrice, BigDecimal tradeVolume) {
        for (Order order : orders) {
            if (order.getLimit().compareTo(tradePrice) < 0) {
                continue;
            }
            walletService.updateWalletByBid(order, tradePrice, tradeVolume);
        }
    }

    /**
     * 매도(ASK): 현재 체결 가격보다 큰 주문은 체결시키지 않는다.
     */
    private void tradeAsk(List<Order> orders, BigDecimal tradePrice, BigDecimal tradeVolume) {
        for (Order order : orders) {
            if (order.getLimit().compareTo(tradePrice) > 0) {
                continue;
            }
            walletService.updateWalletByAsk(order, tradePrice, tradeVolume);
        }
    }
}
