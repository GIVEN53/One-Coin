package OneCoin.Server.order.repository;

import OneCoin.Server.order.entity.Order;
import OneCoin.Server.order.entity.enums.TransactionType;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface OrderRepository extends CrudRepository<Order, Long> {
    List<Order> findAllByUserId(Long userId);

    List<Order> findAllByOrderTypeAndCode(TransactionType orderType, String code);

    List<Order> findAllByUserIdAndOrderTypeAndCode(Long userId, TransactionType orderType, String code);
}
