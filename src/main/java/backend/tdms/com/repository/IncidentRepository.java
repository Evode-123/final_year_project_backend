package backend.tdms.com.repository;

import backend.tdms.com.model.Incident;
import backend.tdms.com.model.Incident.IncidentSeverity;
import backend.tdms.com.model.Incident.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    // Find incidents by reporter
    List<Incident> findByReportedByIdOrderByCreatedAtDesc(Long reporterId);
    
    // ✅ NEW: Find resolved incidents by reporter
    List<Incident> findByReportedByIdAndStatusOrderByResolvedAtDesc(Long reporterId, IncidentStatus status);
    
    // ✅ NEW: Find pending incidents by reporter
    List<Incident> findByReportedByIdAndStatusInOrderByCreatedAtDesc(Long reporterId, List<IncidentStatus> statuses);
    
    // Find incidents by trip
    List<Incident> findByDailyTripIdOrderByIncidentTimeDesc(Long dailyTripId);
    
    // Get latest incidents
    List<Incident> findTop50ByOrderByCreatedAtDesc();
    
    // Get unresolved incidents
    @Query("SELECT i FROM Incident i WHERE i.status IN ('REPORTED', 'ACKNOWLEDGED', 'IN_PROGRESS') ORDER BY i.severity DESC, i.createdAt DESC")
    List<Incident> findUnresolvedIncidents();
    
    // Get critical active incidents
    @Query("SELECT i FROM Incident i WHERE i.severity = 'CRITICAL' AND i.status != 'RESOLVED' AND i.status != 'CANCELLED' ORDER BY i.createdAt DESC")
    List<Incident> findActiveCriticalIncidents();
    
    // Get today's incidents
    @Query("SELECT i FROM Incident i WHERE i.incidentTime >= :startOfDay AND i.incidentTime <= :endOfDay ORDER BY i.incidentTime DESC")
    List<Incident> findTodayIncidents(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    // Statistics queries
    Long countByStatus(IncidentStatus status);
    
    Long countBySeverity(IncidentSeverity severity);
    
    @Query("SELECT COALESCE(SUM(i.delayMinutes), 0) FROM Incident i WHERE i.delayMinutes IS NOT NULL")
    Integer sumTotalDelayMinutes();
    
    @Query("SELECT COALESCE(AVG(i.delayMinutes), 0) FROM Incident i WHERE i.delayMinutes IS NOT NULL AND i.delayMinutes > 0")
    Double averageDelayMinutes();
    
    // ✅ NEW: Count unviewed incidents for a user
    Long countByReportedByIdAndHasUnviewedStatusUpdate(Long reporterId, Boolean hasUnviewed);
}