package OneCoin.Server.utils;

import OneCoin.Server.order.entity.enums.Commission;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CalculationUtil {
    /**
     * 평균 단가를 계산한다.
     */
    public BigDecimal calculateAvgPrice(BigDecimal holdingPrice, BigDecimal holdingAmount, BigDecimal newPrice, BigDecimal newAmount) {
        BigDecimal prevTotal = holdingPrice.multiply(holdingAmount);
        BigDecimal curTotal = newPrice.multiply(newAmount);
        BigDecimal totalAmount = holdingAmount.add(newAmount);

        return prevTotal.add(curTotal).divide(totalAmount, 2, RoundingMode.HALF_UP);
    }

    /**
     * 전일 대비 상승률의 부호를 판별한다.
     */
    public String calculateChangeRate(String price, String prevClosingPrice) {
        BigDecimal curPrice = new BigDecimal(price);
        BigDecimal prePrice = new BigDecimal(prevClosingPrice);
        BigDecimal changeRate = curPrice
                .subtract(prePrice)
                .multiply(new BigDecimal(100))
                .divide(prePrice, 2, RoundingMode.HALF_UP);
        return getSign(changeRate) + changeRate + "%";
    }

    private String getSign(BigDecimal changeRate) {
        int comparison = changeRate.compareTo(BigDecimal.ZERO);
        if (comparison > 0) {
            return "+";
        }
        return "";
    }

    /**
     * 매도(ASK)
     * 수수료를 제외한 총 금액을 계산한다.
     */
    public BigDecimal calculateBySubtractingCommission(BigDecimal price, BigDecimal amount) {
        BigDecimal commissionRate = BigDecimal.ONE.subtract(Commission.ORDER.getRate());
        return price.multiply(amount).multiply(commissionRate);
    }

    /**
     * 매수(BID)
     * 수수료를 더한 총 금액을 계산한다.
     */
    public BigDecimal calculateByAddingCommission(BigDecimal price, BigDecimal amount) {
        BigDecimal commissionRate = BigDecimal.ONE.add(Commission.ORDER.getRate());
        return price.multiply(amount).multiply(commissionRate);
    }

    /**
     * 총 금액의 수수료를 계산한다.
     */
    public BigDecimal calculateOrderCommission(BigDecimal price, BigDecimal amount) {
        return price.multiply(amount).multiply(Commission.ORDER.getRate()).setScale(2, RoundingMode.HALF_UP);
    }
}
