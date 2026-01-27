package backend.tdms.com.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * ✅ Paypack Payment Gateway Configuration with JWT Authentication
 * 
 * How to get credentials:
 * 1. Login to: https://payments.paypack.rw
 * 2. Go to "Applications" → "Create Application"
 * 3. Set name: "Transport Management System"
 * 4. Choose privileges: Cashin, Read & Write
 * 5. Copy client_id and client_secret
 * 6. ⚠️ CRITICAL: Save client_secret immediately (only shown once!)
 */
@Data
@Configuration
public class PaypackConfig {
    
    @Value("${paypack.client.id:}")
    private String clientId;
    
    @Value("${paypack.client.secret:}")
    private String clientSecret;
    
    @Value("${paypack.api.url:https://payments.paypack.rw/api}")
    private String apiUrl;
    
    /**
     * Check if Paypack is properly configured
     */
    public boolean isConfigured() {
        boolean configured = clientId != null && !clientId.trim().isEmpty() 
            && clientSecret != null && !clientSecret.trim().isEmpty();
        
        if (!configured) {
            System.err.println("❌ PAYPACK CONFIGURATION ERROR:");
            System.err.println("   Client ID: " + (clientId == null || clientId.isEmpty() ? "MISSING" : "✓ Present"));
            System.err.println("   Client Secret: " + (clientSecret == null || clientSecret.isEmpty() ? "MISSING" : "✓ Present"));
            System.err.println("");
            System.err.println("   📋 TO GET YOUR CREDENTIALS:");
            System.err.println("   1. Login to: https://payments.paypack.rw");
            System.err.println("   2. Go to 'Applications' menu");
            System.err.println("   3. Click 'Create Application'");
            System.err.println("   4. Fill in:");
            System.err.println("      - Name: Transport Management System");
            System.err.println("      - Description: Payment for bus tickets");
            System.err.println("      - Privileges: ✓ Cashin, ✓ Read & Write");
            System.err.println("   5. Click 'Create'");
            System.err.println("   6. ⚠️ COPY client_secret NOW (only shown once!)");
            System.err.println("   7. Add both to application.properties");
        }
        
        return configured;
    }
}