package backend.tdms.com.service;

import backend.tdms.com.model.Booking;
import backend.tdms.com.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ✅ AUTOMATIC PAYMENT VERIFICATION - IMPROVED VERSION
 * This service automatically checks pending payments every 15 seconds
 * and confirms bookings when payment is successful.
 * 
 * IMPROVEMENTS:
 * - Faster polling (15s instead of 20s)
 * - Better logging with emojis for easy debugging
 * - Handles more payment statuses (completed, cancelled)
 * - Summary statistics after each run
 * 
 * NO USER INTERVENTION NEEDED - Fully automatic!
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentVerificationScheduler {

    private final BookingRepository bookingRepository;
    private final PaypackService paypackService;
    private final BookingService bookingService;

    /**
     * ✅ Runs automatically every 15 seconds for faster confirmation
     * Checks all pending payments from the last 15 minutes
     */
    @Scheduled(fixedDelay = 15000) // Every 15 seconds (reduced from 20)
    @Transactional
    public void verifyPendingPayments() {
        try {
            // Find bookings that are PENDING and have a Paypack reference
            // Only check recent ones (last 15 minutes)
            LocalDateTime fifteenMinutesAgo = LocalDateTime.now().minusMinutes(15);
            
            List<Booking> pendingBookings = bookingRepository.findAll().stream()
                .filter(booking -> 
                    "PENDING".equals(booking.getBookingStatus()) &&
                    "PENDING".equals(booking.getPaymentStatus()) &&
                    booking.getPaypackRef() != null &&
                    !booking.getPaypackRef().isEmpty() &&
                    booking.getBookingDate().isAfter(fifteenMinutesAgo)
                )
                .toList();

            if (pendingBookings.isEmpty()) {
                // No pending payments to check
                log.debug("No pending payments to verify at this time");
                return;
            }

            log.info("🔍 AUTO-VERIFICATION: Checking {} pending payment(s)...", pendingBookings.size());

            int successCount = 0;
            int failedCount = 0;
            int stillPendingCount = 0;

            for (Booking booking : pendingBookings) {
                try {
                    String paypackRef = booking.getPaypackRef();
                    
                    log.info("📋 Verifying: Ticket={}, PaypackRef={}, Age={}s", 
                        booking.getTicketNumber(), 
                        paypackRef,
                        java.time.Duration.between(booking.getBookingDate(), LocalDateTime.now()).getSeconds());
                    
                    // Check payment status from Paypack
                    String status = paypackService.checkPaymentStatus(paypackRef);
                    
                    log.info("💳 Status from Paypack for {}: [{}]", paypackRef, status);
                    
                    // If payment is successful, confirm the booking
                    if ("successful".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status)) {
                        log.info("✅ PAYMENT CONFIRMED! Auto-confirming booking: {}", 
                            booking.getTicketNumber());
                        
                        try {
                            // Call the existing confirmPayment method
                            bookingService.confirmPayment(paypackRef);
                            
                            log.info("🎉🎉🎉 BOOKING CONFIRMED: Ticket={}, PaypackRef={}", 
                                booking.getTicketNumber(), paypackRef);
                            successCount++;
                            
                        } catch (Exception confirmEx) {
                            log.error("❌ Failed to confirm booking {}: {}", 
                                booking.getTicketNumber(), confirmEx.getMessage(), confirmEx);
                        }
                            
                    } else if ("failed".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status)) {
                        log.warn("❌ Payment failed/cancelled for booking: {}", booking.getTicketNumber());
                        
                        // Mark as failed
                        booking.setPaymentStatus("FAILED");
                        booking.setBookingStatus("CANCELLED");
                        booking.setCancellationReason("Payment " + status);
                        bookingRepository.save(booking);
                        failedCount++;
                        
                    } else if ("pending".equalsIgnoreCase(status)) {
                        // Still pending - will check again in next cycle
                        log.debug("⏳ Payment still pending for: {} (will retry)", booking.getTicketNumber());
                        stillPendingCount++;
                    } else {
                        // Unknown status
                        log.warn("⚠️ Unknown payment status '{}' for booking: {}", 
                            status, booking.getTicketNumber());
                        stillPendingCount++;
                    }
                    
                } catch (Exception e) {
                    log.error("❌ Error verifying payment for booking {}: {}", 
                        booking.getTicketNumber(), e.getMessage());
                    // Continue with next booking
                }
            }
            
            // Summary log
            if (successCount > 0 || failedCount > 0) {
                log.info("📊 Verification Summary: ✅ {} confirmed, ❌ {} failed, ⏳ {} still pending", 
                    successCount, failedCount, stillPendingCount);
            }
            
        } catch (Exception e) {
            log.error("❌ Critical error in payment verification scheduler: {}", e.getMessage(), e);
        }
    }
    
    /**
     * ✅ Optional: Clean up very old pending bookings (older than 30 minutes)
     * Runs every 5 minutes
     */
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    @Transactional
    public void cleanupExpiredPendingPayments() {
        try {
            LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
            
            List<Booking> expiredBookings = bookingRepository.findAll().stream()
                .filter(booking -> 
                    "PENDING".equals(booking.getBookingStatus()) &&
                    "PENDING".equals(booking.getPaymentStatus()) &&
                    booking.getBookingDate().isBefore(thirtyMinutesAgo)
                )
                .toList();

            if (!expiredBookings.isEmpty()) {
                log.info("🧹 Cleaning up {} expired pending bookings...", expiredBookings.size());
                
                for (Booking booking : expiredBookings) {
                    booking.setBookingStatus("CANCELLED");
                    booking.setPaymentStatus("EXPIRED");
                    booking.setCancellationReason("Payment timeout - no payment received within 30 minutes");
                    bookingRepository.save(booking);
                    
                    log.info("Cancelled expired booking: {}", booking.getTicketNumber());
                }
            }
            
        } catch (Exception e) {
            log.error("Error in cleanup scheduler: {}", e.getMessage());
        }
    }
}