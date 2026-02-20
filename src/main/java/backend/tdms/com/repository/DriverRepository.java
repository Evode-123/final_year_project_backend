package backend.tdms.com.repository;

import backend.tdms.com.model.Driver;
import backend.tdms.com.model.User;
import backend.tdms.com.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    
    List<Driver> findByStatus(String status);
    
    List<Driver> findByIsBackupFalse();
    
    List<Driver> findByIsBackupTrue();
    
    Optional<Driver> findByLicenseNo(String licenseNo);
    
    Optional<Driver> findByIdNumber(String idNumber);
    
    Optional<Driver> findByAssignedVehicle(Vehicle vehicle);
    
    @Query("SELECT d FROM Driver d WHERE d.assignedVehicle.id = ?1")
    Optional<Driver> findByAssignedVehicleId(Long vehicleId);
    
    boolean existsByLicenseNo(String licenseNo);
    
    boolean existsByIdNumber(String idNumber);
    
    @Query("SELECT d FROM Driver d WHERE d.status = 'ACTIVE' AND d.assignedVehicle IS NULL AND d.isBackup = false")
    List<Driver> findAvailableDriversForAssignment();

    // ✅ NEW: Find driver by User ID (permanent link)
    @Query("SELECT d FROM Driver d WHERE d.user.id = ?1")
    Optional<Driver> findByUserId(Long userId);

    // ✅ FALLBACK: Find by phone (for backward compatibility during migration)
    Optional<Driver> findByPhoneNumber(String phoneNumber);

    Optional<Driver> findByUser(User user);
}