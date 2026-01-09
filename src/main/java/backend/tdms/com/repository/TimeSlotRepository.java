package backend.tdms.com.repository;

import backend.tdms.com.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByIsActiveTrue();
    
    Optional<TimeSlot> findByDepartureTime(LocalTime departureTime);
    
    List<TimeSlot> findByIsActiveTrueOrderByDepartureTimeAsc();
}