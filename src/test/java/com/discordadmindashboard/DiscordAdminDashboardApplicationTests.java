package com.discordadmindashboard;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies the Spring context starts with no Discord credentials configured
 * (the bot connection is optional), so CI can build without secrets.
 */
@SpringBootTest
class DiscordAdminDashboardApplicationTests {

    @Test
    void contextLoads() {
    }
}
