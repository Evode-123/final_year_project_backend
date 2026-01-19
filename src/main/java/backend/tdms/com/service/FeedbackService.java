package backend.tdms.com.service;

import backend.tdms.com.dto.*;
import backend.tdms.com.model.Feedback;
import backend.tdms.com.model.User;
import backend.tdms.com.repository.FeedbackRepository;
import backend.tdms.com.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public FeedbackResponseDTO submitFeedback(SubmitFeedbackDTO dto) {
        log.info("Submitting feedback with rating: {}", dto.getRating());

        Feedback feedback = new Feedback();
        feedback.setRating(dto.getRating());
        feedback.setFeedbackCategory(dto.getFeedbackCategory());
        feedback.setFeedbackText(dto.getFeedbackText());
        feedback.setBookingReference(dto.getBookingReference());
        feedback.setIsAnonymous(dto.getIsAnonymous() != null ? dto.getIsAnonymous() : false);

        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                feedback.setUser(user);
                if (!feedback.getIsAnonymous()) {
                    feedback.setCustomerName(user.getFirstName() + " " + user.getLastName());
                    feedback.setCustomerEmail(user.getEmail());
                    feedback.setCustomerPhone(user.getPhone());
                }
            }
        } catch (Exception e) {
            feedback.setCustomerName(dto.getCustomerName());
            feedback.setCustomerEmail(dto.getCustomerEmail());
            feedback.setCustomerPhone(dto.getCustomerPhone());
        }

        Feedback saved = feedbackRepository.save(feedback);

        if ("NEGATIVE".equals(saved.getSentiment())) {
            sendNegativeFeedbackAlert(saved);
        }

        log.info("Feedback submitted successfully: ID {}, Sentiment: {}", saved.getId(), saved.getSentiment());

        return convertToDTO(saved);
    }

    private void sendNegativeFeedbackAlert(Feedback feedback) {
        try {
            List<User> admins = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream()
                    .anyMatch(r -> "ROLE_ADMIN".equals(r.getName())))
                .collect(Collectors.toList());

            String customerInfo = feedback.getIsAnonymous() ? 
                "Anonymous User" : 
                feedback.getCustomerName() + " (" + feedback.getCustomerEmail() + ")";

            for (User admin : admins) {
                emailService.sendNegativeFeedbackAlert(
                    admin.getEmail(),
                    admin.getFirstName(),
                    feedback.getId(),
                    customerInfo,
                    feedback.getRating(),
                    feedback.getFeedbackCategory(),
                    feedback.getFeedbackText(),
                    feedback.getBookingReference()
                );
            }

            log.info("Negative feedback alerts sent to {} admins", admins.size());
        } catch (Exception e) {
            log.error("Failed to send negative feedback alert: {}", e.getMessage());
        }
    }

    @Transactional
    public FeedbackResponseDTO respondToFeedback(AdminResponseDTO dto) {
        Feedback feedback = feedbackRepository.findById(dto.getFeedbackId())
            .orElseThrow(() -> new RuntimeException("Feedback not found"));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Admin not found"));

        feedback.setAdminResponse(dto.getAdminResponse());
        feedback.setStatus(dto.getStatus());
        feedback.setRespondedBy(admin);
        feedback.setRespondedAt(LocalDateTime.now());
        feedback.setReadByUser(false); // NEW: Mark as unread for user

        Feedback updated = feedbackRepository.save(feedback);

        if (updated.getCustomerEmail() != null && !updated.getIsAnonymous()) {
            try {
                emailService.sendFeedbackResponseNotification(
                    updated.getCustomerEmail(),
                    updated.getCustomerName(),
                    updated.getId(),
                    dto.getAdminResponse()
                );
            } catch (Exception e) {
                log.error("Failed to send feedback response notification: {}", e.getMessage());
            }
        }

        return convertToDTO(updated);
    }

    public List<FeedbackResponseDTO> getAllFeedback() {
        return feedbackRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public List<FeedbackResponseDTO> getPendingNegativeFeedback() {
        return feedbackRepository.findPendingNegativeFeedback().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public List<FeedbackResponseDTO> getFeedbackBySentiment(String sentiment) {
        return feedbackRepository.findBySentiment(sentiment).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public List<FeedbackResponseDTO> getFeedbackByCategory(String category) {
        return feedbackRepository.findByFeedbackCategory(category).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public List<FeedbackResponseDTO> getFeaturedFeedback() {
        return feedbackRepository.findFeaturedPositiveFeedback().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public FeedbackResponseDTO toggleFeatured(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Feedback not found"));

        feedback.setIsFeatured(!feedback.getIsFeatured());
        Feedback updated = feedbackRepository.save(feedback);

        return convertToDTO(updated);
    }

    public Map<String, Object> getFeedbackStatistics() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        Long total = feedbackRepository.count();
        Double avgRating = feedbackRepository.calculateAverageRating(thirtyDaysAgo);
        Long positive = feedbackRepository.countBySentiment("POSITIVE", thirtyDaysAgo);
        Long neutral = feedbackRepository.countBySentiment("NEUTRAL", thirtyDaysAgo);
        Long negative = feedbackRepository.countBySentiment("NEGATIVE", thirtyDaysAgo);
        
        Long pending = feedbackRepository.countByStatus("PENDING");
        Long reviewed = feedbackRepository.countByStatus("REVIEWED");
        Long resolved = feedbackRepository.countByStatus("RESOLVED");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFeedbacks", total);
        stats.put("averageRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
        stats.put("positiveFeedbacks", positive);
        stats.put("neutralFeedbacks", neutral);
        stats.put("negativeFeedbacks", negative);
        stats.put("pendingFeedbacks", pending);
        stats.put("reviewedFeedbacks", reviewed);
        stats.put("resolvedFeedbacks", resolved);

        return stats;
    }

    public List<Feedback> getMyFeedbacks(User user) {
        List<Feedback> feedbacks = feedbackRepository.findByUserOrderByCreatedAtDesc(user);
        
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            List<Feedback> emailFeedbacks = feedbackRepository.findByCustomerEmailOrderByCreatedAtDesc(user.getEmail());
            
            Set<Long> feedbackIds = feedbacks.stream().map(Feedback::getId).collect(Collectors.toSet());
            for (Feedback emailFeedback : emailFeedbacks) {
                if (!feedbackIds.contains(emailFeedback.getId())) {
                    feedbacks.add(emailFeedback);
                }
            }
            
            feedbacks.sort((f1, f2) -> f2.getCreatedAt().compareTo(f1.getCreatedAt()));
        }
        
        return feedbacks;
    }

    // NEW: Get my feedbacks as DTOs
    public List<FeedbackResponseDTO> getMyFeedbacksDTO(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        return getMyFeedbacks(user).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public Long getUnreadNegativeFeedbackCount() {
        return feedbackRepository.countUnreadNegativeFeedback();
    }

    // NEW: Get count of unread responses for a user
    public Long getUnreadResponsesCount(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Long userCount = feedbackRepository.countUnreadResponsesByUser(user);
        Long emailCount = feedbackRepository.countUnreadResponsesByEmail(email);
        
        return userCount + emailCount;
    }

    // NEW: Mark feedback as read by user
    @Transactional
    public void markAsReadByUser(Long feedbackId, String email) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
            .orElseThrow(() -> new RuntimeException("Feedback not found"));
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify the feedback belongs to this user
        if ((feedback.getUser() != null && feedback.getUser().getId().equals(user.getId())) ||
            (feedback.getCustomerEmail() != null && feedback.getCustomerEmail().equals(email))) {
            feedback.setReadByUser(true);
            feedbackRepository.save(feedback);
        } else {
            throw new RuntimeException("Unauthorized to mark this feedback as read");
        }
    }

    private FeedbackResponseDTO convertToDTO(Feedback feedback) {
        FeedbackResponseDTO dto = new FeedbackResponseDTO();
        dto.setId(feedback.getId());
        dto.setCustomerName(feedback.getIsAnonymous() ? "Anonymous" : feedback.getCustomerName());
        dto.setCustomerEmail(feedback.getIsAnonymous() ? null : feedback.getCustomerEmail());
        dto.setRating(feedback.getRating());
        dto.setFeedbackCategory(feedback.getFeedbackCategory());
        dto.setFeedbackText(feedback.getFeedbackText());
        dto.setBookingReference(feedback.getBookingReference());
        dto.setIsAnonymous(feedback.getIsAnonymous());
        dto.setSentiment(feedback.getSentiment());
        dto.setStatus(feedback.getStatus());
        dto.setAdminResponse(feedback.getAdminResponse());
        
        if (feedback.getRespondedBy() != null) {
            dto.setRespondedByEmail(feedback.getRespondedBy().getEmail());
        }
        
        dto.setRespondedAt(feedback.getRespondedAt());
        dto.setIsFeatured(feedback.getIsFeatured());
        dto.setReadByUser(feedback.getReadByUser()); // NEW
        dto.setCreatedAt(feedback.getCreatedAt());
        dto.setUpdatedAt(feedback.getUpdatedAt());

        return dto;
    }
}