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
@EnableMethodSecurity(prePostEnabled = true)
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
                        
                        // DEBUG ENDPOINTS (REMOVE IN PRODUCTION)
                        .requestMatchers("/api/debug/**").permitAll()
                        
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
                        
                        // ✅ CRITICAL: Public read-only for routes, vehicles, timeslots
                        // This allows unauthenticated users to view available routes
                        .requestMatchers("GET", 
                                "/api/routes",
                                "/api/routes/*",
                                "/api/timeslots",
                                "/api/timeslots/*",
                                "/api/vehicles",
                                "/api/vehicles/*"
                        ).permitAll()
                        
                        .requestMatchers("GET", "/api/bookings/ticket/*").permitAll()
                        .requestMatchers("GET", "/api/bookings/customer/*").permitAll()
                        
                        // ========================================
                        // ADMIN-ONLY ENDPOINTS
                        // ========================================
                        .requestMatchers("/api/admin/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/create-user").hasRole("ADMIN")
                        .requestMatchers("/api/admin/manage-user").hasRole("ADMIN")
                        
                        // ========================================
                        // ADMIN & MANAGER ENDPOINTS
                        // ========================================
                        .requestMatchers("/api/admin/trips/**").hasAnyRole("ADMIN", "MANAGER")
                        
                        // ✅ CRITICAL: Route-Vehicle and Route-TimeSlot management
                        // Managers need full access to these for route management
                        .requestMatchers(
                                "/api/route-vehicles/**",
                                "/api/route-timeslots/**"
                        ).hasAnyRole("ADMIN", "MANAGER")
                        
                        .requestMatchers("/api/drivers/**").hasAnyRole("ADMIN", "MANAGER")
                        
                        // ✅ Routes, Vehicles, TimeSlots - Write operations
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
                        .requestMatchers("POST", "/api/bookings", "/api/bookings/with-payment")
                                .hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST", "OTHER_USER")

                        .requestMatchers("POST", "/api/bookings/confirm-payment/*")
                                .hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST", "OTHER_USER")

                        .requestMatchers("GET", "/api/bookings/payment-status/*")
                                .hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST", "OTHER_USER")

                        .requestMatchers("PUT", "/api/bookings/*/cancel")
                                .hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST", "OTHER_USER")

                        .requestMatchers("GET", "/api/bookings/all-history")
                                .hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST")

                        .requestMatchers("GET", "/api/bookings/my-bookings", "/api/bookings/my-history")
                                .hasAnyRole("RECEPTIONIST", "OTHER_USER")

                        .requestMatchers("GET",
                                "/api/bookings/trip/*",
                                "/api/bookings/today"
                        ).hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST")
                        
                        // ========================================
                        // ✅ VEHICLE SAFETY ENDPOINTS - UPDATED!
                        // ========================================
                        
                        // ✅ DRIVER-SPECIFIC ENDPOINTS (my-vehicle)
                        .requestMatchers("GET",
                                "/api/vehicle-inspections/my-vehicle",
                                "/api/vehicle-inspections/my-vehicle/history"
                        ).hasRole("DRIVER")
                        
                        .requestMatchers("GET",
                                "/api/daily-checks/my-vehicle",
                                "/api/daily-checks/my-vehicle/history",
                                "/api/daily-checks/my-vehicle/latest"
                        ).hasRole("DRIVER")
                        
                        // ✅ INSPECTION VIEWING - Allow drivers to view by vehicle ID
                        .requestMatchers("GET",
                                "/api/vehicle-inspections/vehicle/*/latest",
                                "/api/vehicle-inspections/vehicle/*"
                        ).hasAnyRole("DRIVER", "ADMIN", "MANAGER", "RECEPTIONIST")
                        
                        // ✅ DAILY CHECKS VIEWING - Allow drivers
                        .requestMatchers("GET",
                                "/api/daily-checks/vehicle/*/latest",
                                "/api/daily-checks/vehicle/*/history"
                        ).hasAnyRole("DRIVER", "ADMIN", "MANAGER")
                        
                        // ✅ ADMIN/MANAGER ONLY - Recording inspections
                        .requestMatchers("POST", "/api/vehicle-inspections/record")
                                .hasAnyRole("ADMIN", "MANAGER")
                        
                        .requestMatchers("GET",
                                "/api/vehicle-inspections/due-soon",
                                "/api/vehicle-inspections/overdue",
                                "/api/vehicle-inspections/dashboard"
                        ).hasAnyRole("ADMIN", "MANAGER")
                        
                        // ✅ ALL OTHER DAILY CHECK ENDPOINTS
                        .requestMatchers("/api/daily-checks/**")
                                .hasAnyRole("DRIVER", "ADMIN", "MANAGER")
                        
                        // ========================================
                        // PACKAGE DELIVERY ENDPOINTS
                        // ========================================

                        // ✅ PUBLIC: Track package by tracking number (no auth)
                        .requestMatchers("GET", "/api/packages/track/*")
                        .permitAll()

                        // ✅ USER ENDPOINTS: Users can see their own packages
                        .requestMatchers("GET", "/api/packages/my-sent-packages")
                        .hasAnyRole("OTHER_USER", "RECEPTIONIST", "ADMIN", "MANAGER")

                        .requestMatchers("GET", "/api/packages/my-received-packages")
                        .hasAnyRole("OTHER_USER", "RECEPTIONIST", "ADMIN", "MANAGER")

                        .requestMatchers("GET", "/api/packages/my-statistics")
                        .hasAnyRole("OTHER_USER", "RECEPTIONIST", "ADMIN", "MANAGER")

                        // ✅ STAFF ONLY: All other package endpoints
                        .requestMatchers("/api/packages/**")
                        .hasAnyRole("ADMIN", "MANAGER", "RECEPTIONIST")
                        
                        // ========================================
                        // USER PROFILE & SETTINGS
                        // ========================================
                        .requestMatchers("/api/settings/**").authenticated()
                        .requestMatchers("/api/users/me").authenticated()

                        // ========================================
                        // FEEDBACK ENDPOINTS
                        // ========================================
                        .requestMatchers("GET", "/api/feedbacks/published").permitAll()
                        .requestMatchers("/api/feedbacks/**").authenticated()
                        
                        // ========================================
                        // ⚠️ INCIDENT ENDPOINTS - CRITICAL ORDER!
                        // ========================================
                        
                        // ✅ Step 1: Most specific GET endpoints for drivers
                        .requestMatchers("GET", "/api/incidents/my-trips").hasAnyRole("DRIVER", "ADMIN", "MANAGER")
                        .requestMatchers("GET", "/api/incidents/my-incidents/resolved").hasAnyRole("DRIVER", "ADMIN", "MANAGER")
                        .requestMatchers("GET", "/api/incidents/my-incidents/pending").hasAnyRole("DRIVER", "ADMIN", "MANAGER")
                        .requestMatchers("GET", "/api/incidents/my-incidents").hasAnyRole("DRIVER", "ADMIN", "MANAGER")
                        .requestMatchers("GET", "/api/incidents/unviewed-count").hasAnyRole("DRIVER", "ADMIN", "MANAGER")
                        .requestMatchers("GET", "/api/incidents/trip/*").hasAnyRole("DRIVER", "ADMIN", "MANAGER")
                        
                        // ✅ Step 2: Admin/Manager specific GET endpoints
                        .requestMatchers("GET", "/api/incidents/unresolved").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("GET", "/api/incidents/critical").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("GET", "/api/incidents/today").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("GET", "/api/incidents/statistics").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("GET", "/api/incidents").hasAnyRole("ADMIN", "MANAGER")
                        
                        // ✅ Step 3: POST endpoints
                        .requestMatchers("POST", "/api/incidents/report").hasAnyRole("DRIVER", "ADMIN", "MANAGER")
                        
                        // ✅ Step 4: PUT endpoints
                        .requestMatchers("PUT", "/api/incidents/*/mark-viewed").hasAnyRole("DRIVER", "ADMIN", "MANAGER")
                        .requestMatchers("PUT", "/api/incidents/*").hasAnyRole("ADMIN", "MANAGER")
                        
                        // ✅ Step 5: Generic GET by ID (MUST BE LAST!)
                        .requestMatchers("GET", "/api/incidents/*").hasAnyRole("DRIVER", "ADMIN", "MANAGER")

                        // ========================================
                        // TRIP ENDPOINTS (for drivers)
                        // ========================================
                        .requestMatchers("GET", "/api/trips/my-upcoming")
                        .hasRole("DRIVER")
                        
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
                "http://localhost:5173",
                "http://localhost:4200"
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
                "Content-Disposition"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
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