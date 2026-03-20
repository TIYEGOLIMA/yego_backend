package com.yego.backend.config;

import com.yego.backend.service.yego_principal.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

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
    
    @Bean
    /** Cost 10: equilibrio seguridad/velocidad; verificaciones y cambios de contraseña más rápidos. */
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
    
    @Bean(name = "defaultRestTemplate")
    @org.springframework.context.annotation.Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(authService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Permitir localhost para desarrollo
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3030",
            "http://localhost:5173",
            "http://localhost:5174",
            "https://integral.yego.pro",
            "https://api-int.yego.pro",
            "https://neto.yego.pro",
            "https://siscoca.yego.pro",
            "https://ct4.yego.pro"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        // Headers adicionales para WebSocket y autenticación
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh").permitAll()
                .requestMatchers("/api/pro-ops/parks/*/contractor").permitAll() // Contractor suggestions sin auth (CT4, etc.)
                .requestMatchers("/api/users/listado").permitAll() // Listado usuarios sin token
                .requestMatchers("/api/ticketera/auth/refresh").permitAll() // Alias ticketera auth
                .requestMatchers("/api/recaudo/**").permitAll() // Recaudo endpoints
                .requestMatchers("/api/garantizado/**").permitAll() // Garantizado endpoints
                .requestMatchers("/api/flota/**").permitAll() // Flota endpoints
                .requestMatchers("/api/conductor-registro/**").permitAll() // Conductor registro endpoints
                .requestMatchers("/api/drivers/**").permitAll() // Drivers endpoints
                .requestMatchers("/api/roles/**").permitAll() // Roles endpoints
                .requestMatchers("/api/permissions/**").permitAll() // Permisos endpoints
                .requestMatchers("/api/registros/**").permitAll() // Registros endpoints
                .requestMatchers("/api/modules/**").permitAll() // Módulos (CRUD módulos del sidebar)
                .requestMatchers("/api/grupos/**").permitAll() // Grupos (agrupación de módulos)
                .requestMatchers("/api/system/**").permitAll() // Sistema endpoints
                .requestMatchers("/api/asistencia/**").permitAll() // Asistencia (marcación, historial, exportar, etc.)
                .requestMatchers("/api/ticketera/**").permitAll() // Ticketera endpoints
                .requestMatchers("/api/health").permitAll() // Health check
                .requestMatchers("/api/yego-premiun/**").permitAll() // Driver active stats endpoints
                .requestMatchers("/api/marketing-mensajes/**").permitAll() // Marketing mensajes endpoints
                .requestMatchers("/api/pro-ops/**").permitAll() // Pro Ops endpoints
                .requestMatchers("/api/GoBot/**").permitAll() // GoBot API externa endpoints
                .requestMatchers("/ws/**").permitAll() // WebSocket endpoints
                .requestMatchers("/actuator/**").permitAll() // Actuator endpoints
                .requestMatchers("/error").permitAll() // Error endpoint
                .anyRequest().authenticated()
            );
        
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}

