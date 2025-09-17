package com.raimonvibe.imageconverter.config;

import java.util.List;

import com.raimonvibe.imageconverter.security.GoogleIdTokenAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http, GoogleIdTokenAuthFilter googleFilter) throws Exception {
    http
      .csrf(csrf -> csrf
          .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
          .ignoringRequestMatchers("/health", "/api/public/**", "/stripe/webhook"))
      .cors(Customizer.withDefaults())
      .headers(headers -> headers
          .frameOptions(frameOptions -> frameOptions.deny())
          .contentTypeOptions(Customizer.withDefaults())
          .httpStrictTransportSecurity(hstsConfig -> hstsConfig
              .maxAgeInSeconds(31536000)
              .includeSubDomains(true))
          .contentSecurityPolicy(csp -> csp
              .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'")))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers("/health", "/api/public/**", "/stripe/webhook").permitAll()
        .anyRequest().authenticated())
      .addFilterBefore(googleFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of("http://localhost:3000"));
    config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
