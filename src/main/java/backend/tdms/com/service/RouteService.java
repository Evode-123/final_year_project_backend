package backend.tdms.com.service;

import backend.tdms.com.dto.RouteDTO;
import backend.tdms.com.model.Route;
import backend.tdms.com.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;

    @Transactional
    public Route createRoute(RouteDTO dto) {
        if (routeRepository.existsByOriginAndDestination(dto.getOrigin(), dto.getDestination())) {
            throw new RuntimeException("Route already exists");
        }

        Route route = new Route();
        route.setOrigin(dto.getOrigin());
        route.setDestination(dto.getDestination());
        route.setPrice(dto.getPrice());
        route.setDurationMinutes(dto.getDurationMinutes());
        
        // ✅ NEW: Set turnaround buffer (with smart default)
        if (dto.getTurnaroundBufferMinutes() != null) {
            route.setTurnaroundBufferMinutes(dto.getTurnaroundBufferMinutes());
        } else {
            // Auto-calculate buffer based on duration
            route.setTurnaroundBufferMinutes(calculateDefaultBuffer(dto.getDurationMinutes()));
        }
        
        route.setIsActive(true);

        return routeRepository.save(route);
    }

    @Transactional
    public Route updateRoute(Long id, RouteDTO dto) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Route not found"));

        route.setOrigin(dto.getOrigin());
        route.setDestination(dto.getDestination());
        route.setPrice(dto.getPrice());
        route.setDurationMinutes(dto.getDurationMinutes());
        
        // ✅ NEW: Update buffer
        if (dto.getTurnaroundBufferMinutes() != null) {
            route.setTurnaroundBufferMinutes(dto.getTurnaroundBufferMinutes());
        }
        
        route.setIsActive(dto.getIsActive());

        return routeRepository.save(route);
    }

    public List<Route> getAllActiveRoutes() {
        return routeRepository.findByIsActiveTrue();
    }

    public Route getRouteById(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Route not found"));
    }

    @Transactional
    public void deleteRoute(Long id) {
        Route route = getRouteById(id);
        route.setIsActive(false);
        routeRepository.save(route);
    }

    // ✅ NEW METHOD: Smart buffer calculation
    private Integer calculateDefaultBuffer(Integer durationMinutes) {
        if (durationMinutes == null) {
            return 30; // default
        }
        
        if (durationMinutes <= 60) {
            return 15; // Short routes: 15 minutes
        } else if (durationMinutes <= 240) {
            return 30; // Medium routes: 30 minutes
        } else {
            return 60; // Long routes: 60 minutes
        }
    }
}