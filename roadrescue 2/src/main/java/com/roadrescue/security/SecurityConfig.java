package com.roadrescue.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomLoginSuccessHandler customLoginSuccessHandler;
    private final AdminAuthenticationSuccessHandler successHandler;


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protection with cookie-based token
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/api/ws/**") // WebSocket exempt
                )

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/", "/auth/**", "/css/**", "/js/**", "/images/**","/uploads/**",
                                "/error", "/garages/public/**").permitAll()
                        // Admin only
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Garage owner
                        .requestMatchers("/garage-owner/**").hasRole("GARAGE_OWNER")
                        // Technician
                        .requestMatchers("/technician/**").hasRole("TECHNICIAN")
                        // Authenticated users
                        .anyRequest().authenticated()
                )

                // Form login
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .successHandler(successHandler)
                        .failureUrl("/auth/login?error=true")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .permitAll()
                )

                // Logout
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .permitAll()
                )

                // Session management
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .expiredUrl("/auth/login?expired=true")
                )

                // Remember me
                .rememberMe(remember -> remember
                        .key("roadrescue-remember-me-key")
                        .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 days
                )

                // Exception handling
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/error/403")
                );

        return http.build();
    }
}
