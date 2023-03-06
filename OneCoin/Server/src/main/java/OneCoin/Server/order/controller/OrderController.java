package OneCoin.Server.order.controller;

import OneCoin.Server.dto.MultiResponseDto;
import OneCoin.Server.order.dto.OrderDto;
import OneCoin.Server.order.entity.Order;
import OneCoin.Server.order.mapper.OrderMapper;
import OneCoin.Server.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final OrderMapper mapper;

    @PostMapping("/{code}")
    public ResponseEntity postOrder(@PathVariable("code") String code,
                                    @Valid @RequestBody OrderDto.Post orderPostDto,
                                    @AuthenticationPrincipal Map<String, Object> userInfo) {
        long userId = Long.parseLong(userInfo.get("id").toString());
        Order order = mapper.postDtoToOrder(orderPostDto);
        orderService.createOrder(order, userId, code);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/non-trading")
    public ResponseEntity getNonTradingOrder(@AuthenticationPrincipal Map<String, Object> userInfo) {
        long userId = Long.parseLong(userInfo.get("id").toString());
        List<Order> orders = orderService.findOrders(userId);
        List<OrderDto.GetResponse> responseDto = mapper.orderToGetResponse(orders);

        return new ResponseEntity<>(new MultiResponseDto<>(responseDto), HttpStatus.OK);
    }

    @DeleteMapping("/non-trading/{order-id}")
    public ResponseEntity deleteNonTradingOrder(@PathVariable("order-id") long orderId,
                                                @AuthenticationPrincipal Map<String, Object> userInfo) {
        long userId = Long.parseLong(userInfo.get("id").toString());
        orderService.cancelOrder(orderId, userId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
