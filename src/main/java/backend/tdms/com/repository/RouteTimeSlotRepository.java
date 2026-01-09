package backend.tdms.com.repository;

import backend.tdms.com.model.RouteTimeSlot;
import backend.tdms.com.model.Route;
import backend.tdms.com.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RouteTimeSlotRepository extends JpaRepository<RouteTimeSlot, Long> {
    List<RouteTimeSlot> findByRouteAndIsActiveTrue(Route route);
    
    List<RouteTimeSlot> findByIsActiveTrue();
    
    @Query("SELECT rts FROM RouteTimeSlot rts WHERE rts.route.id = ?1 AND rts.isActive = true")
    List<RouteTimeSlot> findActiveByRouteId(Long routeId);
    
    boolean existsByRouteAndTimeSlot(Route route, TimeSlot timeSlot);
}