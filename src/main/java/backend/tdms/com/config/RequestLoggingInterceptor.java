package backend.tdms.com.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        // Only log route-vehicles requests
        if (uri.contains("/route-vehicles")) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            log.error("╔════════════════════════════════════════════════════════════");
            log.error("║ REQUEST TO: {} {}", method, uri);
            log.error("╠════════════════════════════════════════════════════════════");
            
            if (auth == null) {
                log.error("║ ❌ NO AUTHENTICATION FOUND");
            } else {
                log.error("║ ✅ User: {}", auth.getName());
                log.error("║ ✅ Authorities: {}", auth.getAuthorities());
                log.error("║ ✅ Is Authenticated: {}", auth.isAuthenticated());
                
                boolean hasAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                boolean hasManager = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
                    
                log.error("║ 🔐 Has ROLE_ADMIN: {}", hasAdmin);
                log.error("║ 🔐 Has ROLE_MANAGER: {}", hasManager);
                log.error("║ 📋 Expected Result: {}", 
                    (hasAdmin || hasManager) ? "SHOULD ALLOW ✅" : "SHOULD DENY ❌");
            }
            
            log.error("╚════════════════════════════════════════════════════════════");
        }
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        String uri = request.getRequestURI();
        
        // Only log route-vehicles requests
        if (uri.contains("/route-vehicles")) {
            int status = response.getStatus();
            
            log.error("╔════════════════════════════════════════════════════════════");
            log.error("║ RESPONSE FROM: {}", uri);
            log.error("║ Status Code: {} {}", status, getStatusText(status));
            log.error("╚════════════════════════════════════════════════════════════");
            
            if (status >= 400) {
                log.error("❌❌❌ REQUEST FAILED WITH STATUS: {} ❌❌❌", status);
            }
        }
    }
    
    private String getStatusText(int status) {
        return switch (status) {
            case 200 -> "OK ✅";
            case 400 -> "BAD REQUEST ❌";
            case 401 -> "UNAUTHORIZED ❌";
            case 403 -> "FORBIDDEN ❌";
            case 404 -> "NOT FOUND ❌";
            case 500 -> "SERVER ERROR ❌";
            default -> "";
        };
    }
}