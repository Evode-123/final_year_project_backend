package backend.tdms.com.repository;

import backend.tdms.com.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByIsActiveTrue();
    
    List<Vehicle> findByStatus(String status);
    
    Optional<Vehicle> findByPlateNo(String plateNo);
    
    boolean existsByPlateNo(String plateNo);
}