package com.backend.perfumes.config;

import com.backend.perfumes.filter.JwtAuthenticationFilter;
import com.backend.perfumes.services.JwtService;
import com.backend.perfumes.services.UserDetailService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserDetailService userDetailService;

    public SecurityConfig(JwtService jwtService, UserDetailService userDetailService) {
        this.jwtService = jwtService;
        this.userDetailService = userDetailService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailService);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/upload/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers("/verify-account").permitAll()
                        .requestMatchers("/api/auth/verify").permitAll()
                        .requestMatchers("/api/debug/**").permitAll()

                        // PÚBLICOS
                        .requestMatchers(HttpMethod.GET, "/api/perfumes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/perfumes/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/perfumes/public/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/perfumes/buscar").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/brands").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/brands/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/brands/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/brands/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/brands/check").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/public").permitAll()

                        // FAVORITOS - CORREGIDO (sin ROLE_)
                        .requestMatchers(HttpMethod.POST, "/api/favorites/**").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.DELETE, "/api/favorites/**").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/favorites/**").hasRole("CLIENTE")

                        // ÓRDENES - CORREGIDO (sin ROLE_)
                        .requestMatchers(HttpMethod.POST, "/api/orders/checkout").hasAnyRole("CLIENTE", "ADMIN")

                        // VENDEDOR Y ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/categories").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.POST, "/api/brands").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.PUT, "/api/brands/**").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/brands/**").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.GET, "/api/brands/mis-marcas").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.GET, "/api/brands/mis-marcas/**").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.POST, "/api/brands/mis-marcas").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.POST, "/api/brands/mis-marcas/con-imagen").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.GET, "/api/perfumes/mis-perfumes").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.GET, "/api/perfumes/marca/**").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.POST, "/api/perfumes/nuevo").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.POST, "/api/perfumes/nuevo-con-imagen").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.PUT, "/api/perfumes/**").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.PUT, "/api/perfumes/**/con-imagen").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/perfumes/**").hasAnyRole("ADMIN", "VENDEDOR")

                        // SELLER ORDERS - CORREGIDO (sin ROLE_)
                        .requestMatchers(HttpMethod.GET, "/api/seller/orders/**").hasAnyRole("VENDEDOR", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/seller/orders/**").hasAnyRole("VENDEDOR", "ADMIN")

                        // NOTIFICACIONES
                        .requestMatchers(HttpMethod.GET, "/api/notifications/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/notifications/**").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
                .userDetailsService(userDetailService)
                .passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("http://localhost:8100"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}