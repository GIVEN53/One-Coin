package OneCoin.Server.upbit.dto;

import OneCoin.Server.upbit.dto.orderbook.OrderBookDto;
import OneCoin.Server.upbit.dto.ticker.TickerDto;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class MarketDto {
    private List<TickerDto> ticker;
    private List<OrderBookDto> orderBook;
}
