package OneCoin.Server.order.controller;

import OneCoin.Server.dto.MultiResponseDto;
import OneCoin.Server.dto.PageResponseDto;
import OneCoin.Server.order.dto.TransactionHistoryDto;
import OneCoin.Server.order.entity.TransactionHistory;
import OneCoin.Server.order.mapper.TransactionHistoryMapper;
import OneCoin.Server.order.service.TransactionHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Positive;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order/completion")
@RequiredArgsConstructor
@Validated
public class TransactionHistoryController {
    private final int PAGE_SIZE = 10;
    private final TransactionHistoryService transactionHistoryService;
    private final TransactionHistoryMapper mapper;

    @GetMapping
    public ResponseEntity getTransactionHistoryPagination(@RequestParam(name = "period", required = false, defaultValue = "w") String period,
                                                          @RequestParam(name = "type", required = false, defaultValue = "ALL") String type,
                                                          @RequestParam(name = "code", required = false) String code,
                                                          @Positive @RequestParam(name = "page", required = false, defaultValue = "1") int page,
                                                          @AuthenticationPrincipal Map<String, Object> userInfo) {
        long userId = Long.parseLong(userInfo.get("id").toString());
        Page<TransactionHistory> transactionHistoryPage = transactionHistoryService.findTransactionHistory(period, type, code, PageRequest.of(page - 1, PAGE_SIZE, Sort.by("createdAt").descending()), userId);
        List<TransactionHistory> transactionHistories = transactionHistoryPage.getContent();
        List<TransactionHistoryDto.GetResponse> responseDto = mapper.transactionHistoryToGetResponse(transactionHistories);

        return new ResponseEntity<>(new PageResponseDto<>(responseDto, transactionHistoryPage), HttpStatus.OK);
    }

    @GetMapping("/{code}")
    public ResponseEntity getOrderTransactionHistoryByCode(@PathVariable("code") String code,
                                                      @AuthenticationPrincipal Map<String, Object> userInfo) {
        long userId = Long.parseLong(userInfo.get("id").toString());
        List<TransactionHistory> transactionHistories = transactionHistoryService.findOrderTransactionHistory(code, userId);
        List<TransactionHistoryDto.GetResponse> responseDto = mapper.transactionHistoryToGetResponse(transactionHistories);

        return new ResponseEntity<>(new MultiResponseDto<>(responseDto), HttpStatus.OK);
    }
}
