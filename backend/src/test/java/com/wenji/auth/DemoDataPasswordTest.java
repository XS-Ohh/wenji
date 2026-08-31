package com.wenji.auth;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DemoDataPasswordTest {

    private static final String ADMIN_HASH = "$2a$10$/jwTRzorrH2/s9qaHlaYieDFBfAre7nnjXAg7DGYdNXAaPrRb3yAG";
    private static final String STUDENT_HASH = "$2a$10$unSasMoy/SNUUWJOzhVGheFIbJ30jjUguONT4./wMxjGXrEIwCvQK";

    @Test
    void seedHashesMatchDocumentedDemoPasswords() throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String seedSql = new ClassPathResource("data.sql").getContentAsString(StandardCharsets.UTF_8);

        assertThat(encoder.matches("Admin123!", ADMIN_HASH)).isTrue();
        assertThat(encoder.matches("Student123!", STUDENT_HASH)).isTrue();
        assertThat(seedSql).contains(ADMIN_HASH, STUDENT_HASH).doesNotContain("__ADMIN_BCRYPT__", "__STUDENT_BCRYPT__");
    }
}
