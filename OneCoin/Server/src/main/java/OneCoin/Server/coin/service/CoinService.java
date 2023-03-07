package OneCoin.Server.coin.service;

import OneCoin.Server.coin.entity.Coin;
import OneCoin.Server.coin.repository.CoinRepository;
import OneCoin.Server.exception.BusinessLogicException;
import OneCoin.Server.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CoinService {
    private final CoinRepository coinRepository;

    /**
     * 코인을 조회한다.
     * @param code 코인 코드
     * @throws BusinessLogicException 코인이 존재하지 않을 경우
     * @return coin
     */
    @Transactional(readOnly = true)
    public Coin findCoin(String code) {
        Optional<Coin> optionalCoin = coinRepository.findByCode(code);
        return optionalCoin.orElseThrow(() -> new BusinessLogicException(ExceptionCode.COIN_NOT_EXISTS));
    }
}
