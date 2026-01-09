package backend.tdms.com.controller;

import backend.tdms.com.dto.RouteVehicleAssignmentDTO;
import backend.tdms.com.model.RouteVehicle;
import backend.tdms.com.model.Vehicle;
import backend.tdms.com.service.RouteVehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/route-vehicles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RouteVehicleController {
    
    private final RouteVehicleService routeVehicleService;

    @PostMapping("/assign")
    public ResponseEntity<RouteVehicle> assignVehicleToRoute(
            @RequestBody RouteVehicleAssignmentDTO dto) {
        RouteVehicle routeVehicle = routeVehicleService.assignVehicleToRoute(dto);
        return new ResponseEntity<>(routeVehicle, HttpStatus.CREATED);
    }

    @GetMapping("/route/{routeId}/vehicles")
    public ResponseEntity<List<Vehicle>> getVehiclesForRoute(@PathVariable Long routeId) {
        List<Vehicle> vehicles = routeVehicleService.getVehiclesForRoute(routeId);
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping
    public ResponseEntity<List<RouteVehicle>> getAllAssignments() {
        List<RouteVehicle> assignments = routeVehicleService.getActiveAssignments();
        return ResponseEntity.ok(assignments);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeVehicleFromRoute(@PathVariable Long id) {
        routeVehicleService.removeVehicleFromRoute(id);
        return ResponseEntity.noContent().build();
    }
}