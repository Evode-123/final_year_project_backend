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

    // Get all inspections for a vehicle (most recent first)
    @Query("SELECT vi FROM VehicleInspection vi WHERE vi.vehicle.id = :vehicleId ORDER BY vi.inspectionDate DESC")
    List<VehicleInspection> findByVehicleId(@Param("vehicleId") Long vehicleId);

    // Get latest inspection for a vehicle
    @Query("SELECT vi FROM VehicleInspection vi WHERE vi.vehicle.id = :vehicleId ORDER BY vi.inspectionDate DESC LIMIT 1")
    Optional<VehicleInspection> findLatestByVehicleId(@Param("vehicleId") Long vehicleId);

    // Find vehicles due for inspection soon (within 30 days)
    @Query("SELECT vi FROM VehicleInspection vi WHERE vi.nextInspectionDue BETWEEN :today AND :thirtyDaysLater ORDER BY vi.nextInspectionDue ASC")
    List<VehicleInspection> findDueSoon(
        @Param("today") LocalDate today,
        @Param("thirtyDaysLater") LocalDate thirtyDaysLater
    );

    // Find overdue inspections
    @Query("SELECT vi FROM VehicleInspection vi WHERE vi.nextInspectionDue < :today ORDER BY vi.nextInspectionDue ASC")
    List<VehicleInspection> findOverdue(@Param("today") LocalDate today);

    // Count vehicles by status
    @Query("SELECT COUNT(DISTINCT vi.vehicle.id) FROM VehicleInspection vi WHERE vi.inspectionStatus = :status")
    Long countByStatus(@Param("status") String status);
}