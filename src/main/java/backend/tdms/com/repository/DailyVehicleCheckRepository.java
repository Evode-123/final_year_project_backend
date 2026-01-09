package backend.tdms.com.repository;

import backend.tdms.com.model.DailyVehicleCheck;
import backend.tdms.com.model.Driver;
import backend.tdms.com.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyVehicleCheckRepository extends JpaRepository<DailyVehicleCheck, Long> {

    // Get all checks for a vehicle
    List<DailyVehicleCheck> findByVehicleOrderByCheckDateDesc(Vehicle vehicle);

    // Get checks for today
    @Query("SELECT dvc FROM DailyVehicleCheck dvc WHERE dvc.checkDate = :today ORDER BY dvc.createdAt DESC")
    List<DailyVehicleCheck> findTodaysChecks(@Param("today") LocalDate today);

    // Get checks by driver
    List<DailyVehicleCheck> findByDriverOrderByCheckDateDesc(Driver driver);

    // Get checks with problems
    @Query("SELECT dvc FROM DailyVehicleCheck dvc WHERE dvc.hasProblems = true AND dvc.checkDate >= :sinceDate ORDER BY dvc.urgencyLevel DESC, dvc.checkDate DESC")
    List<DailyVehicleCheck> findChecksWithProblems(@Param("sinceDate") LocalDate sinceDate);

    // Get checks requiring action
    @Query("SELECT dvc FROM DailyVehicleCheck dvc WHERE dvc.actionRequired = true AND dvc.followUpNeeded = true ORDER BY dvc.urgencyLevel DESC, dvc.checkDate DESC")
    List<DailyVehicleCheck> findChecksRequiringAction();

    // Get urgent checks
    @Query("SELECT dvc FROM DailyVehicleCheck dvc WHERE dvc.overallStatus = 'URGENT' AND dvc.checkDate >= :sinceDate ORDER BY dvc.checkDate DESC")
    List<DailyVehicleCheck> findUrgentChecks(@Param("sinceDate") LocalDate sinceDate);

    // Get unreviewed checks with problems
    @Query("SELECT dvc FROM DailyVehicleCheck dvc WHERE dvc.hasProblems = true AND dvc.reviewedBy IS NULL ORDER BY dvc.urgencyLevel DESC, dvc.createdAt DESC")
    List<DailyVehicleCheck> findUnreviewedProblems();

    // Get latest check for a vehicle
    @Query("SELECT dvc FROM DailyVehicleCheck dvc WHERE dvc.vehicle.id = :vehicleId ORDER BY dvc.checkDate DESC, dvc.createdAt DESC LIMIT 1")
    Optional<DailyVehicleCheck> findLatestCheckByVehicleId(@Param("vehicleId") Long vehicleId);

    // Check if vehicle was checked today
    @Query("SELECT COUNT(dvc) > 0 FROM DailyVehicleCheck dvc WHERE dvc.vehicle.id = :vehicleId AND dvc.checkDate = :today")
    boolean isVehicleCheckedToday(@Param("vehicleId") Long vehicleId, @Param("today") LocalDate today);

    // Get checks by date range
    List<DailyVehicleCheck> findByCheckDateBetweenOrderByCheckDateDesc(LocalDate startDate, LocalDate endDate);

    // Get vehicle checks by date range
    @Query("SELECT dvc FROM DailyVehicleCheck dvc WHERE dvc.vehicle.id = :vehicleId AND dvc.checkDate BETWEEN :startDate AND :endDate ORDER BY dvc.checkDate DESC")
    List<DailyVehicleCheck> findVehicleChecksByDateRange(
        @Param("vehicleId") Long vehicleId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}