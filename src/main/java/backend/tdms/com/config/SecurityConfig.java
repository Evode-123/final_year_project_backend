package backend.tdms.com.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import backend.tdms.com.service.JwtAuthenticationFilter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Enable @PreAuthorize annotations
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors().and()
                .csrf().disable()
                .authorizeHttpRequests(auth -> auth
                        // ========================================
                        // PUBLIC ENDPOINTS - No authentication
                        // ========================================
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password"
                        ).permitAll()
                        
                        // Public trip search (customers can search)
                        .requestMatchers(
                                "/api/bookings/search",
                                "/api/bookings/available"
                        ).permitAll()
                        
                        // Public ticket access (anyone with ticket can view)
                        .requestMatchers(
                                "/api/bookings/ticket/*/print",
                                "/api/bookings/ticket/*/print-html",
                                "/api/bookings/ticket/*/receipt",
                                "/api/bookings/ticket/*/download",
                                "/api/bookings/ticket/*/download-pdf",
                                "/api/bookings/ticket/*/receipt-pdf"
                        ).permitAll()
                        
                        // Public read-only for routes, vehicles, timeslots
                        .requestMatchers("GET", 
                                "/api/routes",
                                "/api/routes/*",
                                "/api/timeslots",
                                "/api/vehicles"
                        ).permitAll()
                        
                        // ✅ Allow getting booking by ticket number (public)
                        .requestMatchers("GET", "/api/bookings/ticket/*").permitAll()
                        
                        // ✅ Allow customers to view their bookings by phone (public)
                        .requestMatchers("GET", "/api/bookings/customer/*").permitAll()
                        
                        // ========================================
                        // ADMIN-ONLY ENDPOINTS
                        // ========================================
                        // User management (ADMIN only)
                        .requestMatchers("/api/admin/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/create-user").hasRole("ADMIN")
                        .requestMatchers("/api/admin/manage-user").hasRole("ADMIN")
                        
                        // ========================================
                        // ADMIN & MANAGER ENDPOINTS
                        // ========================================
                        // ✅ Trip generation - ADMIN & MANAGER can view, only ADMIN can generate
                        // (Controlled by @PreAuthorize in controller)
                        .requestMatchers("/api/admin/trips/**").hasAnyRole("ADMIN", "MANAGER")
                        
                        // ✅ Route-Vehicle & Route-TimeSlot assignments - ADMIN & MANAGER
                        .requestMatchers("/api/route-vehicles/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/route-timeslots/**").hasAnyRole("ADMIN", "MANAGER")
                        
                        // ✅ Drivers management - ADMIN & MANAGER
                        .requestMatchers("/api/drivers/**").hasAnyRole("ADMIN", "MANAGER")
                        
                        // ✅ Routes, Vehicles, TimeSlots management - ADMIN & MANAGER
                        .requestMatchers("POST", 
                                "/api/routes",
                                "/api/vehicles",
                                "/api/timeslots"
                        ).hasAnyRole("ADMIN", "MANAGER")
                        
                        .requestMatchers("PUT",
                                "/api/routes/**",
                                "/api/vehicles/**",
                                "/api/timeslots/**"
                        ).hasAnyRole("ADMIN", "MANAGER")
                        
                        .requestMatchers("DELETE",
                                "/api/routes/**",
                                "/api/vehicles/**",
                                "/api/timeslots/**"
                        ).hasAnyRole("ADMIN", "MANAGER")
                        
                        // ========================================
                        // BOOKING ENDPOINTS
                        // ========================================
                        // ✅ Create booking - ADMIN, MANAGER, RECEPTIONIST, OTHER_USER
                        .requestMatchers("POST", "/api/bookings")
                                .hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST", "OTHER_USER")
                        
                        // ✅ Cancel booking - ADMIN, MANAGER, RECEPTIONIST, OTHER_USER
                        .requestMatchers("PUT", "/api/bookings/*/cancel")
                                .hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST", "OTHER_USER")
                        
                        // ✅ View bookings - ADMIN, MANAGER, RECEPTIONIST, OTHER_USER
                        .requestMatchers("GET",
                                "/api/bookings/trip/*",
                                "/api/bookings/today"
                        ).hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST", "OTHER_USER")
                        
                        // ========================================
                        // VEHICLE SAFETY ENDPOINTS
                        // ========================================
                        // ✅ Inspections - ADMIN & MANAGER
                        .requestMatchers("/api/vehicle-inspections/**")
                                .hasAnyRole("ADMIN", "MANAGER")
                        
                        // ✅ Daily checks - ADMIN, MANAGER, DRIVER
                        .requestMatchers("/api/daily-checks/**")
                                .hasAnyRole("ADMIN", "MANAGER", "DRIVER")
                        
                        // ========================================
                        // PACKAGE DELIVERY ENDPOINTS (if you have them)
                        // ========================================
                        .requestMatchers("/api/packages/**")
                                .hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST")
                        
                        // ========================================
                        // USER PROFILE & SETTINGS
                        // ========================================
                        .requestMatchers("/api/settings/**").authenticated()
                        .requestMatchers("/api/users/me").authenticated()
                        
                        // ========================================
                        // ALL OTHER REQUESTS
                        // ========================================
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:5173", // Vite default
                "http://localhost:4200"  // Angular default
        ));
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", 
                "Content-Type", 
                "Accept",
                "X-Requested-With"
        ));
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition" // For file downloads
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Cache preflight for 1 hour
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}