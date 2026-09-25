package com.discordadmindashboard;

import com.discordadmindashboard.config.AppProperties;
import com.discordadmindashboard.config.DiscordProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({DiscordProperties.class, AppProperties.class})
public class DiscordAdminDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscordAdminDashboardApplication.class, args);
    }
}
