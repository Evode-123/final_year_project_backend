package backend.tdms.com.service;

import backend.tdms.com.config.PaypackConfig;
import backend.tdms.com.model.PaymentTransaction;
import backend.tdms.com.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ✅ CORRECT: Paypack Payment Service using official API endpoints
 * Based on official Paypack API documentation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaypackService {

    private final PaypackConfig paypackConfig;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    // JWT Token cache
    private String cachedAccessToken = null;
    private String cachedRefreshToken = null;
    private LocalDateTime tokenExpiry = null;

    /**
     * ✅ STEP 1: Authenticate and get JWT token
     */
    private String getAccessToken() {
        try {
            // Check if we have a valid cached token
            if (cachedAccessToken != null && tokenExpiry != null 
                && LocalDateTime.now().isBefore(tokenExpiry)) {
                log.debug("Using cached JWT token (expires at: {})", tokenExpiry);
                return cachedAccessToken;
            }

            // Try to refresh if we have refresh token
            if (cachedRefreshToken != null) {
                try {
                    log.info("Refreshing JWT token...");
                    return refreshAccessToken();
                } catch (Exception e) {
                    log.warn("Token refresh failed, getting new token: {}", e.getMessage());
                }
            }

            // Get new token
            log.info("Authenticating with Paypack to get JWT token...");

            if (!paypackConfig.isConfigured()) {
                throw new RuntimeException("Paypack is not configured. Please check application.properties");
            }

            Map<String, String> authRequest = new HashMap<>();
            authRequest.put("client_id", paypackConfig.getClientId());
            authRequest.put("client_secret", paypackConfig.getClientSecret());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");

            String authUrl = paypackConfig.getApiUrl() + "/auth/agents/authorize";
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(authRequest, headers);

            log.info("POST {}", authUrl);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                authUrl,
                HttpMethod.POST,
                entity,
                Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody == null) {
                throw new RuntimeException("Empty response from Paypack authentication");
            }

            cachedAccessToken = (String) responseBody.get("access");
            cachedRefreshToken = (String) responseBody.get("refresh");
            
            if (cachedAccessToken == null) {
                throw new RuntimeException("No access token received from Paypack");
            }

            tokenExpiry = LocalDateTime.now().plusMinutes(14);

            log.info("✅ Successfully authenticated with Paypack. Token expires at: {}", tokenExpiry);

            return cachedAccessToken;

        } catch (Exception e) {
            log.error("❌ Paypack authentication failed: {}", e.getMessage(), e);
            
            cachedAccessToken = null;
            cachedRefreshToken = null;
            tokenExpiry = null;
            
            throw new RuntimeException("Failed to authenticate with Paypack: " + e.getMessage());
        }
    }

    /**
     * ✅ STEP 2: Refresh JWT token
     */
    private String refreshAccessToken() {
        try {
            String refreshUrl = paypackConfig.getApiUrl() + "/auth/agents/refresh/" + cachedRefreshToken;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            log.info("GET {}", refreshUrl);

            ResponseEntity<Map> response = restTemplate.exchange(
                refreshUrl,
                HttpMethod.GET,
                entity,
                Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody == null) {
                throw new RuntimeException("Empty response from token refresh");
            }

            cachedAccessToken = (String) responseBody.get("access");
            cachedRefreshToken = (String) responseBody.get("refresh");
            tokenExpiry = LocalDateTime.now().plusMinutes(14);

            log.info("✅ JWT token refreshed successfully");

            return cachedAccessToken;

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            
            cachedAccessToken = null;
            cachedRefreshToken = null;
            tokenExpiry = null;
            
            throw e;
        }
    }

    /**
     * ✅ STEP 3: Initiate mobile money payment (Cashin)
     * Endpoint: POST /api/transactions/cashin
     */
    public PaymentTransaction initiateCashin(String phoneNumber, BigDecimal amount) {
        try {
            String accessToken = getAccessToken();

            log.info("Initiating Paypack payment: Phone={}, Amount={}", phoneNumber, amount);

            // Prepare cashin request
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("number", phoneNumber);
            paymentRequest.put("amount", amount.intValue());

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("X-Webhook-Mode", "production"); // or "test" for sandbox

            // Call cashin endpoint
            String cashinUrl = paypackConfig.getApiUrl() + "/transactions/cashin";
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(paymentRequest, headers);

            log.info("POST {}", cashinUrl);
            log.info("Request body: {}", paymentRequest);

            ResponseEntity<Map> response = restTemplate.exchange(
                cashinUrl,
                HttpMethod.POST,
                entity,
                Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody == null) {
                throw new RuntimeException("Empty response from Paypack cashin");
            }

            log.info("Paypack cashin response: {}", responseBody);

            // Extract response fields
            String paypackRef = (String) responseBody.get("ref");
            String status = (String) responseBody.get("status");
            Object amountObj = responseBody.get("amount");
            String kind = (String) responseBody.get("kind");

            if (paypackRef == null) {
                throw new RuntimeException("No payment reference returned from Paypack");
            }

            // Save transaction
            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setPaypackRef(paypackRef);
            transaction.setPhoneNumber(phoneNumber);
            transaction.setAmount(amount);
            transaction.setStatus(status != null ? status.toUpperCase() : "PENDING");
            transaction.setKind(kind != null ? kind : "CASHIN");

            PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);

            log.info("✅ Payment initiated successfully: Ref={}, Status={}", paypackRef, status);
            log.info("   Customer will receive prompt on phone: {}", phoneNumber);

            return savedTransaction;

        } catch (Exception e) {
            log.error("❌ Failed to initiate payment: {}", e.getMessage(), e);
            
            // Save failed transaction
            PaymentTransaction failedTransaction = new PaymentTransaction();
            failedTransaction.setPhoneNumber(phoneNumber);
            failedTransaction.setAmount(amount);
            failedTransaction.setStatus("FAILED");
            failedTransaction.setErrorMessage(e.getMessage());
            
            paymentTransactionRepository.save(failedTransaction);
            
            throw new RuntimeException("Payment initiation failed: " + e.getMessage());
        }
    }

    /**
     * ✅ Check payment status with better error handling
     * Handles "transaction not found" gracefully
     */
    public String checkPaymentStatus(String paypackRef) {
        try {
            String accessToken = getAccessToken();

            log.info("Checking payment status for ref: {}", paypackRef);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + accessToken);

            // Use correct Paypack endpoint
            String findUrl = paypackConfig.getApiUrl() + "/transactions/find/" + paypackRef;

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            log.debug("GET {}", findUrl);

            ResponseEntity<Map> response = restTemplate.exchange(
                findUrl,
                HttpMethod.GET,
                entity,
                Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody != null) {
                String status = (String) responseBody.get("status");
                log.info("✅ Payment status for {}: {}", paypackRef, status);
                
                // Update transaction in database
                updateTransactionFromResponse(paypackRef, responseBody);
                
                return status;
            }
            
            log.warn("Empty response body for {}", paypackRef);
            return "pending";

        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // ✅ Handle 404 "transaction not found" gracefully
            log.warn("Transaction not found in Paypack yet: {}. This is normal for very recent transactions.", paypackRef);
            
            // Check if it exists in our database
            try {
                PaymentTransaction dbTransaction = paymentTransactionRepository
                    .findByPaypackRef(paypackRef)
                    .orElse(null);
                
                if (dbTransaction != null) {
                    log.info("Transaction found in database with status: {}", dbTransaction.getStatus());
                    return dbTransaction.getStatus().toLowerCase();
                }
            } catch (Exception dbEx) {
                log.error("Error checking database: {}", dbEx.getMessage());
            }
            
            // Transaction was just created, Paypack needs time to process
            // Return "pending" instead of "ERROR"
            return "pending";
            
        } catch (Exception e) {
            log.error("Failed to check payment status for {}: {}", paypackRef, e.getMessage());
            
            // ✅ Check database as fallback
            try {
                PaymentTransaction dbTransaction = paymentTransactionRepository
                    .findByPaypackRef(paypackRef)
                    .orElse(null);
                
                if (dbTransaction != null) {
                    log.info("Using status from database: {}", dbTransaction.getStatus());
                    return dbTransaction.getStatus().toLowerCase();
                }
            } catch (Exception dbEx) {
                log.error("Database fallback failed: {}", dbEx.getMessage());
            }
            
            // Return "pending" instead of "ERROR" - payment might still be processing
            return "pending";
        }
    }

    /**
     * ✅ Update transaction in database from Paypack response
     */
    private void updateTransactionFromResponse(String paypackRef, Map<String, Object> responseBody) {
        try {
            PaymentTransaction transaction = paymentTransactionRepository
                .findByPaypackRef(paypackRef)
                .orElse(null);

            if (transaction != null) {
                String status = (String) responseBody.get("status");
                String kind = (String) responseBody.get("kind");

                if (status != null) {
                    transaction.setStatus(status.toUpperCase());
                }
                if (kind != null) {
                    transaction.setKind(kind);
                }

                paymentTransactionRepository.save(transaction);
                log.debug("Transaction updated in database: {}", paypackRef);
            }
        } catch (Exception e) {
            log.error("Failed to update transaction {}: {}", paypackRef, e.getMessage());
        }
    }

    /**
     * Update transaction status manually
     */
    public void updateTransactionStatus(String paypackRef, String status, String errorMessage) {
        try {
            PaymentTransaction transaction = paymentTransactionRepository
                .findByPaypackRef(paypackRef)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + paypackRef));

            transaction.setStatus(status);
            if (errorMessage != null) {
                transaction.setErrorMessage(errorMessage);
            }

            paymentTransactionRepository.save(transaction);
            
            log.info("Transaction updated: Ref={}, Status={}", paypackRef, status);
        } catch (Exception e) {
            log.error("Failed to update transaction {}: {}", paypackRef, e.getMessage());
        }
    }

    /**
     * Clear JWT token cache
     */
    public void clearTokenCache() {
        cachedAccessToken = null;
        cachedRefreshToken = null;
        tokenExpiry = null;
        log.info("JWT token cache cleared");
    }

    /**
     * Test connection
     */
    public boolean testConnection() {
        try {
            String token = getAccessToken();
            return token != null && !token.isEmpty();
        } catch (Exception e) {
            log.error("Connection test failed: {}", e.getMessage());
            return false;
        }
    }
}