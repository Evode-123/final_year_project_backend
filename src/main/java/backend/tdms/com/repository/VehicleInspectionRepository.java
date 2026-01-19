package backend.tdms.com.repository;

import backend.tdms.com.model.VehicleInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleInspectionRepository extends JpaRepository<VehicleInspection, Long> {

    /**
     * Get all inspections for a vehicle (most recent first)
     */
    @Query("SELECT vi FROM VehicleInspection vi WHERE vi.vehicle.id = :vehicleId ORDER BY vi.inspectionDate DESC")
    List<VehicleInspection> findByVehicleId(@Param("vehicleId") Long vehicleId);

    /**
     * ✅ FIXED: Get latest inspection for a vehicle (no LIMIT in JPQL)
     * Use native query instead
     */
    @Query(value = "SELECT * FROM vehicle_inspections WHERE vehicle_id = :vehicleId ORDER BY inspection_date DESC LIMIT 1", 
           nativeQuery = true)
    Optional<VehicleInspection> findLatestByVehicleId(@Param("vehicleId") Long vehicleId);

    /**
     * ✅ FIXED: Find LATEST inspection per vehicle that is due soon
     * Use native SQL with proper JOIN
     */
    @Query(value = "SELECT vi.* FROM vehicle_inspections vi " +
           "INNER JOIN (" +
           "    SELECT vehicle_id, MAX(id) as max_id " +
           "    FROM vehicle_inspections " +
           "    GROUP BY vehicle_id" +
           ") latest ON vi.id = latest.max_id " +
           "WHERE vi.next_inspection_due BETWEEN :startDate AND :endDate " +
           "ORDER BY vi.next_inspection_due ASC", 
           nativeQuery = true)
    List<VehicleInspection> findDueSoon(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * ✅ FIXED: Find LATEST inspection per vehicle that is overdue
     * Use native SQL with proper JOIN
     */
    @Query(value = "SELECT vi.* FROM vehicle_inspections vi " +
           "INNER JOIN (" +
           "    SELECT vehicle_id, MAX(id) as max_id " +
           "    FROM vehicle_inspections " +
           "    GROUP BY vehicle_id" +
           ") latest ON vi.id = latest.max_id " +
           "WHERE vi.next_inspection_due < :today " +
           "ORDER BY vi.next_inspection_due ASC", 
           nativeQuery = true)
    List<VehicleInspection> findOverdue(@Param("today") LocalDate today);

    /**
     * Count vehicles by inspection status
     */
    @Query("SELECT COUNT(DISTINCT vi.vehicle.id) FROM VehicleInspection vi WHERE vi.inspectionStatus = :status")
    Long countByStatus(@Param("status") String status);
}