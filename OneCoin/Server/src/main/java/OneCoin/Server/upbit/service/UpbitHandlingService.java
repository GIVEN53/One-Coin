package OneCoin.Server.upbit.service;

import OneCoin.Server.upbit.dto.orderbook.OrderBookDto;
import OneCoin.Server.upbit.dto.ticker.TickerDto;
import OneCoin.Server.upbit.entity.Trade;
import OneCoin.Server.upbit.entity.UnitInfo;
import OneCoin.Server.upbit.entity.enums.SiseType;
import OneCoin.Server.upbit.mapper.OrderBookDtoMapper;
import OneCoin.Server.upbit.repository.OrderBookRepository;
import OneCoin.Server.upbit.repository.TickerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UpbitHandlingService {
    private final ObjectMapper objectMapper;
    private final OrderBookDtoMapper mapper;
    private final TickerRepository tickerRepository;
    private final OrderBookRepository orderBookRepository;
    private final ApplicationEventPublisher publisher;

    /**
     * upbit에서 전달받은 코인 정보를 parsing한다.
     */
    public void parsing(JsonNode jsonNode) {
        String type = jsonNode.get("type").asText();

        if (type.equals(SiseType.TICKER.getType())) {
            handleTicker(jsonNode);
        }
        if (type.equals(SiseType.TRADE.getType())) {
            handleTrade(jsonNode);
        }
        if (type.equals(SiseType.ORDER_BOOK.getType())) {
            handleOrderBook(jsonNode);
        }
    }

    /**
     * 현재가 정보
     */
    @SneakyThrows
    private void handleTicker(JsonNode jsonNode) {
        TickerDto tickerDto = objectMapper.readValue(jsonNode.toString(), TickerDto.class);
        tickerRepository.saveTicker(tickerDto);
    }

    /**
     * 체결 정보
     */
    @SneakyThrows
    private void handleTrade(JsonNode jsonNode) {
        Trade trade = objectMapper.readValue(jsonNode.toString(), Trade.class);
        publisher.publishEvent(trade);
    }

    /**
     * 호가 정보
     */
    @SneakyThrows
    private void handleOrderBook(JsonNode jsonNode) {
        String code = objectMapper.readValue(jsonNode.get("code").toString(), String.class);
        String prevClosingPrice = tickerRepository.findTickerByCode(code).getPrevClosingPrice();

        List<UnitInfo> unitInfos = Arrays.asList(objectMapper.readValue(jsonNode.get("orderbook_units").toString(), UnitInfo[].class));
        OrderBookDto orderBookDto = mapper.unitInfoToOrderBookDto(unitInfos, prevClosingPrice);

        orderBookDto.setCode(code);
        orderBookDto.setTotalAskSize(objectMapper.readValue(jsonNode.get("total_ask_size").toString(), String.class));
        orderBookDto.setTotalBidSize(objectMapper.readValue(jsonNode.get("total_bid_size").toString(), String.class));
        orderBookRepository.saveOrderBook(orderBookDto);
    }
}
