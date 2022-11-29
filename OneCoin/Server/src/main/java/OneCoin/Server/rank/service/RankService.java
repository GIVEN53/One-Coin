package OneCoin.Server.rank.service;

import OneCoin.Server.order.entity.Wallet;
import OneCoin.Server.order.repository.TransactionHistoryRepository;
import OneCoin.Server.order.repository.WalletRepository;
import OneCoin.Server.rank.dao.UserRoi;
import OneCoin.Server.rank.entity.RankEntity;
import OneCoin.Server.rank.repository.RankRepository;
import OneCoin.Server.upbit.dto.ticker.TickerDto;
import OneCoin.Server.upbit.repository.TickerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankService {
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final TickerRepository tickerRepository;
    private final WalletRepository walletRepository;
    private final RankRepository rankRepository;

    public List<UserRoi> calculateAllRois() {
        Map<Long, UserRoi> allRoi = new HashMap<>();
        //유저별 매수가격 총액 O(n)
        setUserBids(allRoi);
        //유저별 매도가격 총액 O(n)
        setUserAsks(allRoi);
        // 유저별 현재 코인 가격 총액 O(n)
        setCurrentCoinValues(allRoi);
        // 계산 : ROI  = {(매도가격 총액 + 현재 가격 총액) - 매수가격 총액 } / 매수가격 총액 O(n)
        return calculateAll(allRoi);
    }

    public List<UserRoi> calculateTop10() {
        List<UserRoi> allRoi = calculateAllRois();
        return allRoi.stream().sorted().limit(10).collect(Collectors.toList());
    }

    public void update(List<RankEntity> top10) {
        rankRepository.deleteAll();
        rankRepository.saveAll(top10);
    }

    public List<RankEntity> getTop10() {
        return rankRepository.findAll();
    }

    private void setUserBids(Map<Long, UserRoi> allRoi) {
        List<UserRoi> bids = transactionHistoryRepository.findAllSumOfBidSettledAmount();
        for (UserRoi roi : bids) {
            allRoi.put(roi.getUserId(), roi);
        }
    }

    private void setUserAsks(Map<Long, UserRoi> allRoi) {
        List<UserRoi> asks = transactionHistoryRepository.findAllSumOfAskSettledAmount();
        for (UserRoi roi : asks) {
            Long userId = roi.getUserId();
            allRoi.get(userId).setSumOfAsks(roi.getSumOfAsks());
        }
    }

    private void setCurrentCoinValues(Map<Long, UserRoi> allRoi) {
        List<Wallet> wallets = (List<Wallet>) walletRepository.findAll();
        for (Wallet wallet : wallets) {
            Long userId = wallet.getUserId();
            UserRoi userROI = allRoi.get(userId);
            TickerDto ticker = tickerRepository.findTickerByCode(wallet.getCode());

            double currentValueOfCoinOfUser = wallet.getAmount().doubleValue() * Double.valueOf(ticker.getTradePrice());
            userROI.addSumOfCurrentCoinValues(currentValueOfCoinOfUser);
        }
    }

    private List<UserRoi> calculateAll(Map<Long, UserRoi> allRoi) {
        List<UserRoi> rois = new ArrayList<>(allRoi.values());
        for (UserRoi userROI : rois) {
            userROI.calculate();
        }
        return rois;
    }
}
