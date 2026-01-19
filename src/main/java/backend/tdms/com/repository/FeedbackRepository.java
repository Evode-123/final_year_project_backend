package backend.tdms.com.repository;

import backend.tdms.com.model.Feedback;
import backend.tdms.com.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    
    List<Feedback> findByStatus(String status);
    
    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.status = :status")
    Long countByStatus(@Param("status") String status);
    
    List<Feedback> findBySentiment(String sentiment);
    
    List<Feedback> findByFeedbackCategory(String category);
    
    @Query("SELECT f FROM Feedback f WHERE f.sentiment = 'NEGATIVE' AND f.status = 'PENDING' ORDER BY f.createdAt DESC")
    List<Feedback> findPendingNegativeFeedback();
    
    @Query("SELECT f FROM Feedback f WHERE f.isFeatured = true AND f.sentiment = 'POSITIVE' ORDER BY f.createdAt DESC")
    List<Feedback> findFeaturedPositiveFeedback();
    
    @Query("SELECT f FROM Feedback f WHERE f.createdAt BETWEEN :startDate AND :endDate ORDER BY f.createdAt DESC")
    List<Feedback> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT f FROM Feedback f WHERE f.user.id = :userId ORDER BY f.createdAt DESC")
    List<Feedback> findByUserId(Long userId);
    
    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.createdAt >= :startDate")
    Double calculateAverageRating(LocalDateTime startDate);
    
    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.sentiment = :sentiment AND f.createdAt >= :startDate")
    Long countBySentiment(String sentiment, LocalDateTime startDate);

    List<Feedback> findByUserOrderByCreatedAtDesc(User user);
    
    List<Feedback> findByCustomerEmailOrderByCreatedAtDesc(String email);
    
    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.sentiment = 'NEGATIVE' AND f.status = 'PENDING'")
    Long countUnreadNegativeFeedback();

    // ✅ NEW: Count unread responses for a specific user (logged in)
    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.user = :user AND f.adminResponse IS NOT NULL AND f.readByUser = false")
    Long countUnreadResponsesByUser(@Param("user") User user);

    // ✅ NEW: Count unread responses by email (for users who submitted feedback before logging in)
    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.customerEmail = :email AND f.user IS NULL AND f.adminResponse IS NOT NULL AND f.readByUser = false")
    Long countUnreadResponsesByEmail(@Param("email") String email);

    // ✅ NEW: Find feedbacks with unread responses for a logged-in user
    @Query("SELECT f FROM Feedback f WHERE f.user = :user AND f.adminResponse IS NOT NULL AND f.readByUser = false ORDER BY f.respondedAt DESC")
    List<Feedback> findUnreadResponsesByUser(@Param("user") User user);

    // ✅ NEW: Find feedbacks with unread responses by email (for non-logged in submissions)
    @Query("SELECT f FROM Feedback f WHERE f.customerEmail = :email AND f.adminResponse IS NOT NULL AND f.readByUser = false ORDER BY f.respondedAt DESC")
    List<Feedback> findUnreadResponsesByEmail(@Param("email") String email);
}