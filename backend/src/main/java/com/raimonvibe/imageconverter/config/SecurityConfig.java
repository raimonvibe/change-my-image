package com.raimonvibe.imageconverter.config;

import com.raimonvibe.imageconverter.security.GoogleIdTokenAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, GoogleIdTokenAuthFilter googleFilter) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/health", "/api/public/**", "/stripe/webhook").permitAll()
            .anyRequest().authenticated())
        .addFilterBefore(googleFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
