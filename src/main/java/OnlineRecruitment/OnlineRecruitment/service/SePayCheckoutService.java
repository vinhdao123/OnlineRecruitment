package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.config.SePayGatewayConfig;
import OnlineRecruitment.OnlineRecruitment.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SePayCheckoutService {
    private final SePayGatewayConfig config;
    /** Keep the provider's field order and sign raw UTF-8 values, before URL encoding. */
    public Map<String, String> form(Payment invoice) {
        if (!config.paymentMethod().equals(invoice.getPaymentMethod()) || !"PENDING".equals(invoice.getStatus()))
            throw new IllegalArgumentException("Hóa đơn không thể thanh toán qua cổng này.");
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("order_amount", BigDecimal.valueOf(invoice.getAmount()).toBigIntegerExact().toString());
        fields.put("merchant", config.getMerchantId());
        fields.put("currency", "VND");
        fields.put("operation", "PURCHASE");
        fields.put("order_description", "Thanh toan goi ORPAY" + invoice.getId());
        fields.put("order_invoice_number", "ORPAY" + invoice.getId());
        fields.put("customer_id", "USER" + invoice.getUser().getId());
        fields.put("payment_method", "BANK_TRANSFER");
        String returnUrl = config.publicBaseUrl() + "/pricing/resume/" + invoice.getId();
        fields.put("success_url", returnUrl + "?result=success");
        fields.put("error_url", returnUrl + "?result=error");
        fields.put("cancel_url", returnUrl + "?result=cancel");
        String signed = fields.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(","));
        fields.put("signature", config.sign(signed));
        return fields;
    }
}
