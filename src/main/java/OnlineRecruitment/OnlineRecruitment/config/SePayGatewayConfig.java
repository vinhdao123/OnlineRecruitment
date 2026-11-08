package OnlineRecruitment.OnlineRecruitment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/** Payment Gateway credentials are distinct from bank balance webhook credentials. */
@Component
public class SePayGatewayConfig {
    @Value("${SEPAY_MERCHANT_ID:}") private String merchantId;
    @Value("${SEPAY_MERCHANT_SECRET:}") private String merchantSecret;
    @Value("${SEPAY_IPN_SECRET:}") private String ipnSecret;
    @Value("${SEPAY_ENVIRONMENT:sandbox}") private String environment;
    @Value("${PAYMENT_PUBLIC_BASE_URL:}") private String publicBaseUrl;

    public String getMerchantId() { return merchantId; }
    public boolean isSandbox() { return "sandbox".equals(environment); }
    public String paymentMethod() { return isSandbox() ? "SEPAY_SANDBOX" : "SEPAY_LIVE"; }
    public String checkoutUrl() {
        if (!isReady()) throw new IllegalStateException("Chưa cấu hình đầy đủ cổng thanh toán SePay.");
        return isSandbox() ? "https://pay-sandbox.sepay.vn/v1/checkout/init" : "https://pay.sepay.vn/v1/checkout/init";
    }
    public String publicBaseUrl() { return publicBaseUrl.replaceAll("/+$", ""); }
    public boolean isReady() {
        if (merchantId.isBlank() || merchantSecret.length() < 16 || ipnSecret.length() < 24
                || !("sandbox".equals(environment) || "production".equals(environment))) return false;
        if (!isSandbox() && (merchantId.startsWith("SP-TEST-") || merchantSecret.contains("_test_"))) return false;
        try {
            URI uri = URI.create(publicBaseUrl);
            String host = uri.getHost();
            return "https".equals(uri.getScheme()) && host != null
                    && !host.equalsIgnoreCase("localhost") && !host.equals("127.0.0.1") && !host.equals("[::1]")
                    && uri.getRawUserInfo() == null && uri.getRawQuery() == null && uri.getRawFragment() == null
                    && (uri.getPath().isEmpty() || uri.getPath().equals("/"));
        } catch (IllegalArgumentException e) { return false; }
    }
    public boolean authenticIpn(String provided) {
        return isReady() && provided != null && MessageDigest.isEqual(
                ipnSecret.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8));
    }
    public String sign(String fields) {
        if (!isReady()) throw new IllegalStateException("Chưa cấu hình đầy đủ cổng thanh toán SePay.");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(merchantSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(fields.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Không thể tạo chữ ký thanh toán.", e);
        }
    }
}
