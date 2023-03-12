package OneCoin.Server.order.service;

import OneCoin.Server.coin.entity.Coin;
import OneCoin.Server.coin.repository.CoinRepository;
import OneCoin.Server.coin.service.CoinService;
import OneCoin.Server.exception.BusinessLogicException;
import OneCoin.Server.helper.StubData;
import OneCoin.Server.order.entity.Order;
import OneCoin.Server.order.entity.TransactionHistory;
import OneCoin.Server.order.entity.enums.TransactionType;
import OneCoin.Server.order.repository.TransactionHistoryRepository;
import OneCoin.Server.user.entity.User;
import OneCoin.Server.user.repository.UserRepository;
import OneCoin.Server.user.service.UserService;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@MockBean(OkHttpClient.class)
public class TransactionHistoryServiceTest {
    private final PageRequest pageRequest = PageRequest.of(0, 15, Sort.by("createdAt").descending());
    @Autowired
    private TransactionHistoryService transactionHistoryService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CoinRepository coinRepository;
    @MockBean
    private CoinService coinService;
    @MockBean
    private UserService userService;
    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;
    private final User user = StubData.MockUser.getMockEntity();
    private final Coin coin = StubData.MockCoin.getMockEntity(1L, "KRW-BTC", "비트코인");

    @BeforeEach
    void saveTransactionHistory() {
        userRepository.save(user);
        coinRepository.save(coin);
    }

    @AfterEach
    void deleteAll() {
        transactionHistoryRepository.deleteAll();
    }

    @Test
    @DisplayName("거래 내역을 생성한다.")
    void createTransactionHistory() {
        // given
        Order order = StubData.MockOrder.getMockEntity();
        order.setAmount(BigDecimal.ZERO);

        given(userService.findVerifiedUser(anyLong())).willReturn(user);
        given(coinService.findCoin(anyString())).willReturn(coin);

        // when
        transactionHistoryService.createTransactionHistoryByOrder(order);

        // then
        List<TransactionHistory> transactionHistories = transactionHistoryRepository.findTop10ByUserAndCoinAndTransactionTypeOrderByCreatedAtDesc(user, coin, TransactionType.BID);
        TransactionHistory transactionHistory = transactionHistories.get(0);
        assertThat(transactionHistory.getTransactionType()).isEqualTo(TransactionType.BID);
        assertThat(transactionHistory.getAmount()).isEqualTo(new BigDecimal("10.000000000000000"));
    }

    @ParameterizedTest
    @CsvSource(value = {"w:BID:KRW-BTC:1", "m:ASK::0", "3m:ALL:KRW-BTC:1", "6m:ALL::1"}, delimiter = ':')
    void searchTest(String period, String type, String code, int expectSize) {
        // given
        given(coinService.findCoin(anyString())).willReturn(coin);
        transactionHistoryRepository.save(StubData.MockHistory.getMockEntity(TransactionType.BID));

        // when
        Page<TransactionHistory> transactionHistoryPage = transactionHistoryService.findTransactionHistory(period, type, code, pageRequest, 1L);
        List<TransactionHistory> transactionHistories = transactionHistoryPage.getContent();

        // then
        assertThat(transactionHistories.size()).isEqualTo(expectSize);
    }

    @Test
    @DisplayName("잘못된 기간을 보내면 에러가 발생한다")
    void periodExceptionTest() {
        // given
        String period = "a";

        // when then
        assertThrows(BusinessLogicException.class, () -> transactionHistoryService.findTransactionHistory(period, "BID", "KRW-BTC", pageRequest, 1L));
    }

    @Test
    @DisplayName("잘못된 타입을 입력하면 에러가 발생한다")
    void typeExceptionTest() {
        // given
        String type = "ABC";

        // when then
        assertThrows(BusinessLogicException.class, () -> transactionHistoryService.findTransactionHistory("w", type, "KRW-BTC", pageRequest, 1L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALL", "BID", "ASK", "DEPOSIT", "SWAP"})
    @DisplayName("지정한 타입만 메서드를 에러없이 실행한다")
    void typeTest(String type) {
        assertDoesNotThrow(() -> transactionHistoryService.findTransactionHistory("w", type, "KRW-BTC", pageRequest, 1L));
    }

    @Test
    @DisplayName("최근 순서로 매수, 매도 내역을 조회한다.")
    void orderTransactionHistoriesTest() throws InterruptedException {
        // given
        TransactionHistory transactionHistory1 = StubData.MockHistory.getMockEntity(TransactionType.BID);
        TransactionHistory transactionHistory2 = StubData.MockHistory.getMockEntity(TransactionType.ASK);
        TransactionHistory transactionHistory3 = StubData.MockHistory.getMockEntity(TransactionType.DEPOSIT);

        transactionHistoryRepository.save(transactionHistory1);
        Thread.sleep(1000);
        transactionHistoryRepository.save(transactionHistory2);
        Thread.sleep(1000);
        transactionHistoryRepository.save(transactionHistory3);
        given(coinService.findCoin(anyString())).willReturn(coin);

        // when
        List<TransactionHistory> transactionHistories = transactionHistoryService.findOrderTransactionHistory("KRW-BTC", 1L);

        // then
        assertThat(transactionHistories.size()).isEqualTo(2);
        assertThat(transactionHistories.get(0).getTransactionHistoryId()).isEqualTo(2);
    }

    @Test
    @DisplayName("매수, 매도 내역에서 최근 순서로 10개까지 조회한다.")
    void getTop10Test() throws InterruptedException {
        // given
        for (int i = 0; i < 20; i++) {
            TransactionHistory transactionHistory = StubData.MockHistory.getMockEntity(TransactionType.BID);
            transactionHistoryRepository.save(transactionHistory);
            Thread.sleep(100);
        }
        given(coinService.findCoin(anyString())).willReturn(coin);

        // when
        List<TransactionHistory> transactionHistories = transactionHistoryService.findOrderTransactionHistory("KRW-BTC", 1L);

        // then
        assertThat(transactionHistories.size()).isEqualTo(10);
        assertThat(transactionHistories.get(0).getTransactionHistoryId()).isEqualTo(20);
    }
}
