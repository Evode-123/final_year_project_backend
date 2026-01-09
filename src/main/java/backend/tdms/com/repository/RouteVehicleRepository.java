package backend.tdms.com.repository;

import backend.tdms.com.model.RouteVehicle;
import backend.tdms.com.model.Route;
import backend.tdms.com.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RouteVehicleRepository extends JpaRepository<RouteVehicle, Long> {
    List<RouteVehicle> findByRouteAndIsActiveTrue(Route route);
    
    List<RouteVehicle> findByVehicleAndIsActiveTrue(Vehicle vehicle);
    
    @Query("SELECT rv FROM RouteVehicle rv WHERE rv.route.id = ?1 AND rv.isActive = true")
    List<RouteVehicle> findActiveVehiclesByRouteId(Long routeId);
    
    @Query("SELECT rv.vehicle FROM RouteVehicle rv WHERE rv.route.id = ?1 AND rv.isActive = true")
    List<Vehicle> findVehiclesByRouteId(Long routeId);
    
    boolean existsByRouteAndVehicle(Route route, Vehicle vehicle);
}