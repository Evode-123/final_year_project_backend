package backend.tdms.com.controller;

import backend.tdms.com.dto.VehicleDTO;
import backend.tdms.com.model.Vehicle;
import backend.tdms.com.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Vehicle> createVehicle(@RequestBody VehicleDTO vehicleDTO) {
        Vehicle vehicle = vehicleService.createVehicle(vehicleDTO);
        return new ResponseEntity<>(vehicle, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Vehicle>> getAllActiveVehicles() {
        List<Vehicle> vehicles = vehicleService.getAllActiveVehicles();
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/available")
    public ResponseEntity<List<Vehicle>> getAvailableVehicles() {
        List<Vehicle> vehicles = vehicleService.getAvailableVehicles();
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> getVehicleById(@PathVariable Long id) {
        Vehicle vehicle = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(vehicle);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Vehicle> updateVehicle(@PathVariable Long id, @RequestBody VehicleDTO vehicleDTO) {
        Vehicle vehicle = vehicleService.updateVehicle(id, vehicleDTO);
        return ResponseEntity.ok(vehicle);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }
}