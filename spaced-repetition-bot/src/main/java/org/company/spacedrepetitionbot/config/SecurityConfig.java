package org.company.spacedrepetitionbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Value("${encryption.password}")
    private String password;
    @Value("${encryption.salt}")
    private String salt;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/webhook/github").disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/webhook/github", "/admin/force-sync", "/actuator/health")
                        .permitAll()
                        .requestMatchers("/actuator/**").authenticated()
                        .anyRequest()
                        .authenticated());
        return http.build();
    }

    @Bean
    public TextEncryptor textEncryptor() {
        return Encryptors.delux(password, salt);
    }
}
