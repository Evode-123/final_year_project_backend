package backend.tdms.com.service;

import backend.tdms.com.dto.RouteVehicleAssignmentDTO;
import backend.tdms.com.model.Route;
import backend.tdms.com.model.RouteVehicle;
import backend.tdms.com.model.Vehicle;
import backend.tdms.com.repository.RouteRepository;
import backend.tdms.com.repository.RouteVehicleRepository;
import backend.tdms.com.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteVehicleService {

    private final RouteVehicleRepository routeVehicleRepository;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional
    public RouteVehicle assignVehicleToRoute(RouteVehicleAssignmentDTO dto) {
        Route route = routeRepository.findById(dto.getRouteId())
                .orElseThrow(() -> new RuntimeException("Route not found"));

        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        // Check if already assigned
        if (routeVehicleRepository.existsByRouteAndVehicle(route, vehicle)) {
            throw new RuntimeException("Vehicle already assigned to this route");
        }

        RouteVehicle routeVehicle = new RouteVehicle();
        routeVehicle.setRoute(route);
        routeVehicle.setVehicle(vehicle);
        routeVehicle.setIsActive(true);

        return routeVehicleRepository.save(routeVehicle);
    }

    public List<Vehicle> getVehiclesForRoute(Long routeId) {
        return routeVehicleRepository.findVehiclesByRouteId(routeId);
    }

    public List<RouteVehicle> getActiveAssignments() {
        return routeVehicleRepository.findAll().stream()
                .filter(rv -> rv.getIsActive())
                .toList();
    }

    @Transactional
    public void removeVehicleFromRoute(Long routeVehicleId) {
        RouteVehicle routeVehicle = routeVehicleRepository.findById(routeVehicleId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        
        routeVehicle.setIsActive(false);
        routeVehicleRepository.save(routeVehicle);
    }
}