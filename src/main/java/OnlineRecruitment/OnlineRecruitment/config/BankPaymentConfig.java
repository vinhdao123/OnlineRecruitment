package OnlineRecruitment.OnlineRecruitment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/** Deployment secrets belong in environment variables, never in source control. */
@Component
public class BankPaymentConfig {
    @Value("${PAYMENT_BANK_CODE:}") private String bankCode;
    @Value("${PAYMENT_ACCOUNT_NUMBER:}") private String accountNumber;
    @Value("${PAYMENT_ACCOUNT_NAME:}") private String accountName;
    @Value("${SEPAY_WEBHOOK_API_KEY:}") private String webhookApiKey;

    public String getBankCode() { return bankCode; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountName() { return accountName; }
    public String getWebhookApiKey() { return webhookApiKey; }
    public boolean isReady() {
        return bankCode.matches("[A-Za-z0-9]{2,20}") && accountNumber.matches("[A-Za-z0-9]{5,30}")
                && !accountName.isBlank() && webhookApiKey.length() >= 24;
    }
    public String qrUrl(Long paymentId, Double amount) {
        if (!isReady()) return null;
        return UriComponentsBuilder.fromUriString("https://img.vietqr.io/image/" + bankCode + "-" + accountNumber + "-compact2.png")
                .queryParam("amount", java.math.BigDecimal.valueOf(amount).toBigIntegerExact())
                .queryParam("addInfo", "ORPAY" + paymentId)
                .queryParam("accountName", accountName).build().encode().toUriString();
    }
}
