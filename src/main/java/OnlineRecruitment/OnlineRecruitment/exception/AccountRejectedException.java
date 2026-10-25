package OnlineRecruitment.OnlineRecruitment.exception;

import org.springframework.security.authentication.DisabledException;

public class AccountRejectedException extends DisabledException {
    public AccountRejectedException(String msg) {
        super(msg);
    }
}
