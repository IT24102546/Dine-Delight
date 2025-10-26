package org.example.dinedelightsystems.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.example.dinedelightsystems.service.EmailService;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/register", "/login", "/error").permitAll()
                        .requestMatchers("/menu", "/menu/**").permitAll()
                        .requestMatchers("/cart/**", "/checkout/**", "/orders/**", "/inquiries/**").authenticated()
                        .requestMatchers("/kitchen/**").hasRole("KITCHEN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login").permitAll()
                        .successHandler(authenticationSuccessHandler())
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String username = authentication.getName();
            try {
                EmailService emailService = request.getServletContext()
                        .getAttribute(EmailService.class.getName()) != null
                        ? (EmailService) request.getServletContext().getAttribute(EmailService.class.getName())
                        : null;
            } catch (Exception ignored) {}

            // Send email via Spring Context
            try {
                var ctx = org.springframework.web.context.support.WebApplicationContextUtils
                        .getRequiredWebApplicationContext(request.getServletContext());
                var emailService = ctx.getBean(EmailService.class);
                emailService.sendLoginSuccessEmail(username);
            } catch (Exception e) {
                // ignore email failures for login flow
            }
            if (username != null && username.toLowerCase().endsWith("@delight.com")) {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard");
            } else {
                response.sendRedirect(request.getContextPath() + "/dashboard");
            }
        };
    }
}




