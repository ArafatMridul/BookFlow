package com.example.bookflowproject.config;

import com.example.bookflowproject.security.JwtAuthenticationFilter;
import com.example.bookflowproject.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // Success handler: sets JWT cookie and redirects based on role
    @Bean
    public AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            // Generate JWT and set cookie (best-effort; session auth is primary for web pages)
            try {
                String token = jwtTokenProvider.generateToken(authentication);

                // Determine if request is secure (HTTPS) - also check forwarded headers
                // Behind a reverse proxy (Render, Railway, etc.), request.isSecure() may be false
                // even though the client is using HTTPS. Check X-Forwarded-Proto header too.
                boolean isSecure = request.isSecure()
                        || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));

                // Build Set-Cookie header properly
                StringBuilder cookie = new StringBuilder();
                cookie.append(String.format("jwt=%s; HttpOnly; Path=/; Max-Age=%d; SameSite=Lax", token, 86400));
                if (isSecure) {
                    cookie.append("; Secure");
                }
                response.addHeader("Set-Cookie", cookie.toString());
                log.debug("JWT cookie set successfully for user: {}", authentication.getName());
            } catch (Exception e) {
                // Don't fail the login if JWT generation fails - session auth still works
                log.error("Failed to generate/set JWT cookie for user: {}. Error: {}",
                        authentication.getName(), e.getMessage(), e);
            }

            // Role-based redirect (always happens, even if JWT failed)
            String redirectUrl = "/user/dashboard";
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String role = authority.getAuthority();
                if (role.equals("ROLE_ADMIN")) {
                    redirectUrl = "/admin/dashboard";
                    break;
                } else if (role.equals("ROLE_LIBRARIAN")) {
                    redirectUrl = "/librarian/dashboard";
                    break;
                }
            }
            response.sendRedirect(redirectUrl);
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // JWT filter only for API routes
        OncePerRequestFilter apiOnlyJwtFilter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                if (request.getRequestURI().startsWith("/api/")) {
                    jwtAuthenticationFilter.authenticateRequest(request);
                }
                filterChain.doFilter(request, response);
            }
        };

        http
                // CSRF: exempt login, logout, and API routes
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**", "/login", "/logout")
                )
                // Authorizations
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/error",
                                "/css/**", "/js/**", "/webjars/**", "/images/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/librarian/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers("/api/user/**").hasAnyRole("USER", "LIBRARIAN", "ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/librarian/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers("/user/**").hasAnyRole("USER", "LIBRARIAN", "ADMIN")
                        .requestMatchers("/dashboard", "/profile").authenticated()
                        .anyRequest().authenticated()
                )
                // Form login config
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(roleBasedSuccessHandler())
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                // Logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .deleteCookies("jwt", "JSESSIONID")
                        .permitAll()
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(apiOnlyJwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}