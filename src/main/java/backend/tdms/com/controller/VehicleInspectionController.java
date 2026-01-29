package backend.tdms.com.controller;

import backend.tdms.com.dto.RecordInspectionDTO;
import backend.tdms.com.dto.VehicleInspectionDTO;
import backend.tdms.com.model.Driver;
import backend.tdms.com.model.User;
import backend.tdms.com.repository.UserRepository;
import backend.tdms.com.service.DriverService;
import backend.tdms.com.service.VehicleInspectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/vehicle-inspections")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VehicleInspectionController {

    private final VehicleInspectionService inspectionService;
    private final DriverService driverService;  // ✅ Use DriverService instead of DriverRepository
    private final UserRepository userRepository;

    /**
     * Record a new inspection
     */
    @PostMapping("/record")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> recordInspection(@RequestBody RecordInspectionDTO dto) {
        try {
            VehicleInspectionDTO inspection = inspectionService.recordInspection(dto);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Inspection recorded successfully");
            response.put("inspection", inspection);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error recording inspection: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get latest inspection for a vehicle
     */
    @GetMapping("/vehicle/{vehicleId}/latest")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DRIVER')")
    public ResponseEntity<?> getLatestInspection(@PathVariable Long vehicleId) {
        try {
            VehicleInspectionDTO inspection = inspectionService.getLatestInspection(vehicleId);
            return ResponseEntity.ok(inspection);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Get vehicle inspection history
     */
    @GetMapping("/vehicle/{vehicleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DRIVER')")
    public ResponseEntity<List<VehicleInspectionDTO>> getVehicleInspectionHistory(
            @PathVariable Long vehicleId
    ) {
        List<VehicleInspectionDTO> inspections = inspectionService.getVehicleInspectionHistory(vehicleId);
        return ResponseEntity.ok(inspections);
    }

    /**
     * ✅ FIXED: Get driver's vehicle inspection status
     * Uses DriverService.getDriverByUser() with auto-migration
     */
    @GetMapping("/my-vehicle")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<?> getDriverVehicleInspectionStatus() {
        try {
            log.info("Driver requesting vehicle inspection status");
            
            String currentUserEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();
            
            // Find user
            User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // ✅ FIXED: Use DriverService.getDriverByUser() with auto-migration
            Driver driver = driverService.getDriverByUser(user);
            
            // Check if driver has assigned vehicle
            if (driver.getAssignedVehicle() == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "No vehicle assigned to you");
                response.put("hasVehicle", false);
                return ResponseEntity.ok(response);
            }
            
            // Get vehicle inspection info
            Long vehicleId = driver.getAssignedVehicle().getId();
            
            try {
                VehicleInspectionDTO latestInspection = inspectionService.getLatestInspection(vehicleId);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("hasVehicle", true);
                response.put("vehicle", Map.of(
                    "id", driver.getAssignedVehicle().getId(),
                    "plateNo", driver.getAssignedVehicle().getPlateNo(),
                    "vehicleType", driver.getAssignedVehicle().getVehicleType()
                ));
                response.put("latestInspection", latestInspection);
                
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                // No inspection found
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("hasVehicle", true);
                response.put("vehicle", Map.of(
                    "id", driver.getAssignedVehicle().getId(),
                    "plateNo", driver.getAssignedVehicle().getPlateNo(),
                    "vehicleType", driver.getAssignedVehicle().getVehicleType()
                ));
                response.put("latestInspection", null);
                response.put("message", "No inspection records found for this vehicle");
                
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            log.error("Error getting driver vehicle inspection: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * ✅ FIXED: Get driver's vehicle inspection history
     * Uses DriverService.getDriverByUser() with auto-migration
     */
    @GetMapping("/my-vehicle/history")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<?> getDriverVehicleInspectionHistory() {
        try {
            log.info("Driver requesting vehicle inspection history");
            
            String currentUserEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();
            
            // Find user
            User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // ✅ FIXED: Use DriverService.getDriverByUser() with auto-migration
            Driver driver = driverService.getDriverByUser(user);
            
            // Check if driver has assigned vehicle
            if (driver.getAssignedVehicle() == null) {
                log.warn("Driver {} has no assigned vehicle", driver.getNames());
                return ResponseEntity.ok(List.of());
            }
            
            // Get inspection history
            Long vehicleId = driver.getAssignedVehicle().getId();
            List<VehicleInspectionDTO> inspections = inspectionService.getVehicleInspectionHistory(vehicleId);
            
            log.info("Driver {} retrieved {} inspections for vehicle {}", 
                driver.getNames(), inspections.size(), driver.getAssignedVehicle().getPlateNo());
            
            return ResponseEntity.ok(inspections);
            
        } catch (Exception e) {
            log.error("Error getting driver vehicle inspection history: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get vehicles due for inspection soon
     */
    @GetMapping("/due-soon")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<VehicleInspectionDTO>> getVehiclesDueSoon() {
        List<VehicleInspectionDTO> inspections = inspectionService.getVehiclesDueSoon();
        return ResponseEntity.ok(inspections);
    }

    /**
     * Get overdue vehicles
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<VehicleInspectionDTO>> getOverdueVehicles() {
        List<VehicleInspectionDTO> inspections = inspectionService.getOverdueVehicles();
        return ResponseEntity.ok(inspections);
    }

    /**
     * Get dashboard summary
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        Map<String, Object> summary = inspectionService.getDashboardSummary();
        return ResponseEntity.ok(summary);
    }
}