package backend.tdms.com.controller;

import backend.tdms.com.dto.DriverDTO;
import backend.tdms.com.dto.DriverVehicleAssignmentDTO;
import backend.tdms.com.model.Driver;
import backend.tdms.com.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping
    public ResponseEntity<Driver> createDriver(@RequestBody DriverDTO driverDTO) {
        Driver driver = driverService.createDriver(driverDTO);
        return new ResponseEntity<>(driver, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping
    public ResponseEntity<List<DriverDTO>> getAllDrivers() {
        List<DriverDTO> drivers = driverService.getAllDrivers();
        return ResponseEntity.ok(drivers);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/active")
    public ResponseEntity<List<DriverDTO>> getActiveDrivers() {
        List<DriverDTO> drivers = driverService.getActiveDrivers();
        return ResponseEntity.ok(drivers);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/backup")
    public ResponseEntity<List<DriverDTO>> getBackupDrivers() {
        List<DriverDTO> drivers = driverService.getBackupDrivers();
        return ResponseEntity.ok(drivers);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/available")
    public ResponseEntity<List<DriverDTO>> getAvailableDrivers() {
        List<DriverDTO> drivers = driverService.getAvailableDriversForAssignment();
        return ResponseEntity.ok(drivers);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/{id}")
    public ResponseEntity<DriverDTO> getDriverById(@PathVariable Long id) {
        DriverDTO driver = driverService.getDriverById(id);
        return ResponseEntity.ok(driver);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<DriverDTO> getDriverByVehicle(@PathVariable Long vehicleId) {
        DriverDTO driver = driverService.getDriverByVehicle(vehicleId);
        return ResponseEntity.ok(driver);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<Driver> updateDriver(
            @PathVariable Long id,
            @RequestBody DriverDTO driverDTO) {
        Driver driver = driverService.updateDriver(id, driverDTO);
        return ResponseEntity.ok(driver);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/assign")
    public ResponseEntity<Driver> assignDriverToVehicle(
            @RequestBody DriverVehicleAssignmentDTO dto) {
        Driver driver = driverService.assignDriverToVehicle(dto);
        return ResponseEntity.ok(driver);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/{id}/unassign")
    public ResponseEntity<Driver> unassignDriver(@PathVariable Long id) {
        Driver driver = driverService.unassignDriverFromVehicle(id);
        return ResponseEntity.ok(driver);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/{id}/status")
    public ResponseEntity<Driver> changeDriverStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        Driver driver = driverService.changeDriverStatus(id, status);
        return ResponseEntity.ok(driver);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDriver(@PathVariable Long id) {
        driverService.deleteDriver(id);
        return ResponseEntity.noContent().build();
    }
}