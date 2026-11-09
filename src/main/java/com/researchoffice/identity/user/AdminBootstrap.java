package com.researchoffice.identity.user;

import com.researchoffice.identity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the first ADMIN account when the user table is empty (RO_ADMIN_EMAIL / RO_ADMIN_PASSWORD). */
@Slf4j
@Component
@RequiredArgsConstructor
class AdminBootstrap implements ApplicationRunner {

    static final String DEV_PASSWORD = "ChangeMe#2026";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapProperties props;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0 || props.adminEmail() == null || props.adminPassword() == null) {
            return;
        }
        users.save(new User(props.adminEmail(), props.adminName(), passwordEncoder.encode(props.adminPassword()), Role.ADMIN));
        log.info("Created first admin account {}", props.adminEmail());
        if (DEV_PASSWORD.equals(props.adminPassword())) {
            log.warn("The admin uses the DEV default password - set RO_ADMIN_PASSWORD before going live");
        }
    }

    @ConfigurationProperties("ro.bootstrap")
    record BootstrapProperties(String adminEmail, String adminPassword, String adminName) {
        BootstrapProperties {
            adminName = adminName == null ? "Admin" : adminName;
        }
    }
}
