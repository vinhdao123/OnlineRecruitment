package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.config.SePayGatewayConfig;
import OnlineRecruitment.OnlineRecruitment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequiredArgsConstructor
public class SePayIpnController {
    private final SePayGatewayConfig config;
    private final PaymentService payments;
    private static final Pattern INVOICE = Pattern.compile("ORPAY([1-9][0-9]{0,17})");
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public record Order(String order_invoice_number, String order_status, String order_currency, BigDecimal order_amount) {}
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public record Transaction(String id, String transaction_status, String transaction_type, String transaction_currency, BigDecimal transaction_amount) {}
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public record Notification(String notification_type, Order order, Transaction transaction) {}

    @PostMapping("/api/payments/sepay/ipn")
    public Map<String, Boolean> receive(@RequestHeader(value = "X-Secret-Key", defaultValue = "") String key,
                                       @RequestBody Notification notice) {
        if (!config.isReady()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Gateway not configured");
        if (!config.authenticIpn(key)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (!"ORDER_PAID".equals(notice.notification_type())) {
            // Voids/refunds require reconciliation; do not grant subscriptions.
            return Map.of("success", true);
        }
        Order order = notice.order();
        Transaction transaction = notice.transaction();
        if (order == null || transaction == null || !"CAPTURED".equals(order.order_status())
                || !"APPROVED".equals(transaction.transaction_status()) || !"PAYMENT".equals(transaction.transaction_type())
                || !"VND".equals(order.order_currency()) || !"VND".equals(transaction.transaction_currency())
                || order.order_amount() == null || transaction.transaction_amount() == null
                || order.order_amount().signum() <= 0 || order.order_amount().compareTo(transaction.transaction_amount()) != 0
                || transaction.id() == null || !transaction.id().matches("[A-Za-z0-9_-]{1,64}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid paid notification");
        var match = INVOICE.matcher(order.order_invoice_number() == null ? "" : order.order_invoice_number());
        if (!match.matches()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid invoice number");
        try {
            payments.receiveGatewayPayment(Long.valueOf(match.group(1)), transaction.id(), order.order_amount(), config.paymentMethod());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        }
        return Map.of("success", true);
    }
}
