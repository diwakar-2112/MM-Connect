package com.MM_CONNECT.api.admin.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Opt-in only. Nothing is seeded by adding the source or applying V8. */
@Component
@ConditionalOnProperty(name = "security.staff.bootstrap.enabled", havingValue = "true")
public class StaffBootstrapRunner implements ApplicationRunner {
    private final StaffAuthService auth;
    private final String username;
    private final String password;
    public StaffBootstrapRunner(StaffAuthService auth,
            @Value("${security.staff.bootstrap.username:}") String username,
            @Value("${security.staff.bootstrap.password:}") String password) {
        this.auth = auth; this.username = username; this.password = password;
    }
    @Override public void run(ApplicationArguments args) { auth.bootstrapSuperadmin(username, password); }
}
