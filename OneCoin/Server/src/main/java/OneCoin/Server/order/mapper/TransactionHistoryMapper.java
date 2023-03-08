package OneCoin.Server.order.mapper;

import OneCoin.Server.coin.entity.Coin;
import OneCoin.Server.deposit.entity.Deposit;
import OneCoin.Server.order.dto.TransactionHistoryDto;
import OneCoin.Server.order.entity.Order;
import OneCoin.Server.order.entity.TransactionHistory;
import OneCoin.Server.swap.entity.Swap;
import OneCoin.Server.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TransactionHistoryMapper {
    List<TransactionHistoryDto.GetResponse> transactionHistoryToGetResponse(List<TransactionHistory> transactionHistories);

    @Mapping(target = "completedTime", source = "createdAt")
    @Mapping(target = "orderType", source = "transactionType")
    @Mapping(target = "code", source = "coin.code")
    TransactionHistoryDto.GetResponse entityToDto(TransactionHistory transactionHistory);

    @Mapping(target = "transactionType", source = "order.orderType")
    @Mapping(target = "orderTime", source = "order.orderTime")
    @Mapping(target = "amount", source = "order.amount")
    @Mapping(target = "price", source = "order.limit")
    TransactionHistory orderToTransactionHistory(Order order, User user, Coin coin, BigDecimal totalAmount, double commission, BigDecimal settledAmount);

    @Mapping(target = "transactionType", constant = "SWAP")
    @Mapping(target = "coin", source = "swap.givenCoin")
    @Mapping(target = "amount", source = "swap.givenAmount")
    @Mapping(target = "price", source = "swap.givenCoinPrice")
    @Mapping(target = "orderTime", expression = "java(java.time.LocalDateTime.now())")
    TransactionHistory swapToTransactionHistory(Swap swap, BigDecimal totalAmount, BigDecimal settledAmount);

    @Mapping(target = "transactionType", constant = "DEPOSIT")
    @Mapping(target = "amount", source = "depositAmount")
    @Mapping(target = "price", constant = "0")
    @Mapping(target = "totalAmount", source = "depositAmount")
    @Mapping(target = "commission", constant = "0")
    @Mapping(target = "settledAmount", source = "depositAmount")
    @Mapping(target = "user", source = "balance.user")
    @Mapping(target = "orderTime", expression = "java(java.time.LocalDateTime.now())")
    TransactionHistory depositToTransactionHistory(Deposit deposit);
}
