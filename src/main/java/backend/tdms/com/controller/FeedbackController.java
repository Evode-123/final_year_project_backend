package backend.tdms.com.controller;

import backend.tdms.com.dto.*;
import backend.tdms.com.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * Submit feedback (PUBLIC - anyone can submit)
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitFeedback(@RequestBody SubmitFeedbackDTO dto) {
        try {
            log.info("Received feedback submission with rating: {}", dto.getRating());
            FeedbackResponseDTO feedback = feedbackService.submitFeedback(dto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Thank you for your feedback!");
            response.put("feedback", feedback);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error submitting feedback: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get all feedback (ADMIN & MANAGER)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<FeedbackResponseDTO>> getAllFeedback() {
        List<FeedbackResponseDTO> feedbacks = feedbackService.getAllFeedback();
        return ResponseEntity.ok(feedbacks);
    }

    /**
     * Get pending negative feedback (ADMIN & MANAGER)
     */
    @GetMapping("/pending-negative")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<FeedbackResponseDTO>> getPendingNegativeFeedback() {
        List<FeedbackResponseDTO> feedbacks = feedbackService.getPendingNegativeFeedback();
        return ResponseEntity.ok(feedbacks);
    }

    /**
     * Get count of unread negative feedback (ADMIN & MANAGER) - NEW
     */
    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Long>> getUnreadNegativeFeedbackCount() {
        Long count = feedbackService.getUnreadNegativeFeedbackCount();
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    /**
     * Get my feedbacks (Authenticated users) - NEW
     */
    @GetMapping("/my-feedbacks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FeedbackResponseDTO>> getMyFeedbacks(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<FeedbackResponseDTO> feedbacks = feedbackService.getMyFeedbacksDTO(userDetails.getUsername());
            return ResponseEntity.ok(feedbacks);
        } catch (Exception e) {
            log.error("Error getting user feedbacks: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get count of unread responses for current user - NEW
     */
    @GetMapping("/my-unread-responses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getMyUnreadResponsesCount(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long count = feedbackService.getUnreadResponsesCount(userDetails.getUsername());
            Map<String, Long> response = new HashMap<>();
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting unread responses count: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Mark feedback as read by user - NEW
     */
    @PutMapping("/{id}/mark-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> markFeedbackAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            feedbackService.markAsReadByUser(id, userDetails.getUsername());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Feedback marked as read");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error marking feedback as read: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get feedback by sentiment (ADMIN & MANAGER)
     */
    @GetMapping("/sentiment/{sentiment}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<FeedbackResponseDTO>> getFeedbackBySentiment(@PathVariable String sentiment) {
        List<FeedbackResponseDTO> feedbacks = feedbackService.getFeedbackBySentiment(sentiment);
        return ResponseEntity.ok(feedbacks);
    }

    /**
     * Get feedback by category (ADMIN & MANAGER)
     */
    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<FeedbackResponseDTO>> getFeedbackByCategory(@PathVariable String category) {
        List<FeedbackResponseDTO> feedbacks = feedbackService.getFeedbackByCategory(category);
        return ResponseEntity.ok(feedbacks);
    }

    /**
     * Get featured positive feedback (PUBLIC)
     */
    @GetMapping("/featured")
    public ResponseEntity<List<FeedbackResponseDTO>> getFeaturedFeedback() {
        List<FeedbackResponseDTO> feedbacks = feedbackService.getFeaturedFeedback();
        return ResponseEntity.ok(feedbacks);
    }

    /**
     * Respond to feedback (ADMIN & MANAGER)
     */
    @PostMapping("/respond")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> respondToFeedback(@RequestBody AdminResponseDTO dto) {
        try {
            FeedbackResponseDTO feedback = feedbackService.respondToFeedback(dto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Response submitted successfully");
            response.put("feedback", feedback);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error responding to feedback: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Toggle featured status (ADMIN)
     */
    @PutMapping("/{id}/toggle-featured")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleFeatured(@PathVariable Long id) {
        try {
            FeedbackResponseDTO feedback = feedbackService.toggleFeatured(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Featured status updated");
            response.put("feedback", feedback);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error toggling featured: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get feedback statistics (ADMIN & MANAGER)
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getFeedbackStatistics() {
        Map<String, Object> stats = feedbackService.getFeedbackStatistics();
        return ResponseEntity.ok(stats);
    }
}