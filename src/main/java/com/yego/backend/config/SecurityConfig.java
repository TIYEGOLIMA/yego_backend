package com.yego.backend.config;

import com.yego.backend.service.yego_principal.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.time.Duration;

/**
 * Configuración de seguridad Spring Security 6
 * Equivalente a la configuración de guards y JWT en NestJS
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final AuthService authService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtRequestFilter jwtRequestFilter;
    
    @Bean(name = "defaultRestTemplate")
    @org.springframework.context.annotation.Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(authService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${security.cors.allowed-origin-patterns}") String allowedOriginPatterns) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Access-Token"));
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/ticketera/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register").hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR")
                .requestMatchers("/api/users/profile", "/api/users/listado").authenticated()
                .requestMatchers("/api/users/**").hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR")
                .requestMatchers("/api/roles/**", "/api/permissions/**", "/api/modules/**")
                    .hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR")
                .requestMatchers("/api/sessions/**", "/api/audit/**", "/api/configurations/**", "/api/reports/**")
                    .hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR")
                .requestMatchers("/api/marketing-mensajes/**").authenticated()
                .requestMatchers("/api/system/health", "/api/health", "/actuator/health").permitAll()
                .requestMatchers("/api/system/**", "/actuator/**")
                    .hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR")
                .requestMatchers("/api/recaudo/**").permitAll() // Recaudo endpoints
                .requestMatchers("/api/garantizado/**").permitAll() // Garantizado endpoints
                .requestMatchers("/api/flota/**").permitAll() // Flota endpoints
                .requestMatchers("/api/conductor-registro/**").permitAll() // Conductor registro endpoints
                .requestMatchers("/api/drivers/**").permitAll() // Drivers endpoints
                .requestMatchers("/api/registros/**").permitAll() // Registros endpoints
                .requestMatchers("/api/ticketera/**").permitAll() // Ticketera endpoints
                .requestMatchers("/api/yego-premium/**").permitAll() // Driver active stats endpoints
                .requestMatchers("/api/vehicles/**").permitAll() // Flotas/Vehículos endpoints
                .requestMatchers("/api/mobile/auth/**").permitAll() // OTP/login app móvil Pro Ops
                .requestMatchers("/api/mobile/admin/**").hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR") // Admin móvil
                .requestMatchers("/api/mobile/**").authenticated() // App móvil Pro Ops protegida por JWT
                .requestMatchers("/api/pro-ops/**", "/pro-ops/**").permitAll() // Pro Ops endpoints (con o sin /api si context-path=/api)
                .requestMatchers("/api/GoBot/**").permitAll() // GoBot API externa endpoints
                .requestMatchers("/api/yango-external/**").permitAll() // Yango resumen externo (yego_api_externo)
                .requestMatchers("/ws/**").permitAll() // WebSocket endpoints
                .requestMatchers("/error").permitAll() // Error endpoint
                .anyRequest().authenticated()
            );
        
        http.authenticationProvider(authenticationProvider);
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
