package backend.tdms.com.dto;

import lombok.Data;

@Data
public class FeedbackStatsDTO {
    private Long totalFeedbacks;
    private Double averageRating;
    private Long positiveFeedbacks;
    private Long neutralFeedbacks;
    private Long negativeFeedbacks;
    private Long pendingFeedbacks;
    private Long reviewedFeedbacks;
    private Long resolvedFeedbacks;
}