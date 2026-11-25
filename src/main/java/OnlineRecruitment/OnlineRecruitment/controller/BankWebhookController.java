package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.config.BankPaymentConfig;
import OnlineRecruitment.OnlineRecruitment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequiredArgsConstructor

public class BankWebhookController {
    private final BankPaymentConfig config;
    private final PaymentService payments;
    private static final Pattern INVOICE = Pattern.compile("(?i)(?<![a-z0-9])ORPAY([1-9][0-9]{0,17})(?![a-z0-9])");

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transfer(
            Long id, // Có thể null
            String accountNumber,
            String transferType,
            BigDecimal transferAmount,
            String content,
            String referenceCode // Thêm trường này để hứng dữ liệu từ SePay
    ) {}

    @PostMapping("/api/payments/sepay/webhook")
    public Map<String, Boolean> webhook(@RequestHeader(value = "Authorization", defaultValue = "") String authorization,
                                        @RequestBody Transfer transfer) {

        System.out.println("=== DEBUG TÀI KHOẢN ===");
        System.out.println("Từ SePay: [" + transfer.accountNumber() + "]");
        System.out.println("Từ Config: [" + config.getAccountNumber() + "]");
        if (!config.isReady()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payments not configured");
        byte[] expected = ("Apikey " + config.getWebhookApiKey()).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, authorization.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (!config.getAccountNumber().equals(transfer.accountNumber()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unexpected beneficiary");
        if ("out".equals(transfer.transferType())) return Map.of("success", true);
        if (!"in".equals(transfer.transferType())
                || transfer.transferAmount() == null || transfer.transferAmount().signum() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid transfer");
        var matcher = INVOICE.matcher(transfer.content() == null ? "" : transfer.content());
        if (!matcher.find()) return Map.of("success", true); // unrelated bank transaction
        Long invoiceId = Long.valueOf(matcher.group(1));
        if (matcher.find()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ambiguous invoice reference");
        try {
            // Tạo ID giả lập từ referenceCode hoặc timestamp
            Long referenceId = transfer.referenceCode() != null ? (long) transfer.referenceCode().hashCode() : System.currentTimeMillis();
            payments.receiveBankTransfer(invoiceId, referenceId, transfer.transferAmount());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        }
        return Map.of("success", true);
    }
}
