package backend.tdms.com.repository;

import backend.tdms.com.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findByIsActiveTrue();
    
    List<Route> findByOriginAndDestination(String origin, String destination);
    
    boolean existsByOriginAndDestination(String origin, String destination);
}