package backend.tdms.com.controller;

import backend.tdms.com.dto.RecordInspectionDTO;
import backend.tdms.com.dto.VehicleInspectionDTO;
import backend.tdms.com.model.Driver;
import backend.tdms.com.model.User;
import backend.tdms.com.repository.DriverRepository;
import backend.tdms.com.repository.UserRepository;
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
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;

    /**
     * Record a new inspection (ADMIN/MANAGER ONLY)
     * POST /api/vehicle-inspections/record
     */
    @PostMapping("/record")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> recordInspection(@RequestBody RecordInspectionDTO dto) {
        try {
            log.info("Recording inspection for vehicle ID: {}", dto.getVehicleId());
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
     * ✅ NEW: Get current driver's assigned vehicle inspection status
     * GET /api/vehicle-inspections/my-vehicle
     */
    @GetMapping("/my-vehicle")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<?> getMyVehicleInspectionStatus() {
        try {
            String currentUserEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();
            
            log.info("Driver {} requesting their vehicle inspection status", currentUserEmail);
            
            // Find user
            User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Find driver by matching phone number
            Driver driver = driverRepository.findAll().stream()
                .filter(d -> d.getPhoneNumber() != null && 
                           user.getPhone() != null && 
                           d.getPhoneNumber().equals(user.getPhone()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Driver profile not found"));
            
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
            VehicleInspectionDTO inspection;
            
            try {
                inspection = inspectionService.getLatestInspection(vehicleId);
            } catch (Exception e) {
                // Vehicle never inspected
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("hasVehicle", true);
                response.put("vehicle", Map.of(
                    "id", driver.getAssignedVehicle().getId(),
                    "plateNo", driver.getAssignedVehicle().getPlateNo(),
                    "vehicleType", driver.getAssignedVehicle().getVehicleType()
                ));
                response.put("inspection", null);
                response.put("message", "This vehicle has never been inspected");
                return ResponseEntity.ok(response);
            }
            
            // Return vehicle and inspection info
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hasVehicle", true);
            response.put("vehicle", Map.of(
                "id", driver.getAssignedVehicle().getId(),
                "plateNo", driver.getAssignedVehicle().getPlateNo(),
                "vehicleType", driver.getAssignedVehicle().getVehicleType(),
                "capacity", driver.getAssignedVehicle().getCapacity()
            ));
            response.put("inspection", inspection);
            
            log.info("Driver {} vehicle {} - Inspection status: {}, Due: {}", 
                driver.getNames(), 
                driver.getAssignedVehicle().getPlateNo(),
                inspection.getUrgency(),
                inspection.getNextInspectionDue()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting driver vehicle inspection: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * ✅ NEW: Get inspection history for driver's assigned vehicle
     * GET /api/vehicle-inspections/my-vehicle/history
     */
    @GetMapping("/my-vehicle/history")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<?> getMyVehicleInspectionHistory() {
        try {
            String currentUserEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();
            
            // Find user
            User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Find driver by matching phone number
            Driver driver = driverRepository.findAll().stream()
                .filter(d -> d.getPhoneNumber() != null && 
                           user.getPhone() != null && 
                           d.getPhoneNumber().equals(user.getPhone()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Driver profile not found"));
            
            // Check if driver has assigned vehicle
            if (driver.getAssignedVehicle() == null) {
                return ResponseEntity.ok(List.of());
            }
            
            // Get inspection history
            Long vehicleId = driver.getAssignedVehicle().getId();
            List<VehicleInspectionDTO> history = inspectionService.getVehicleInspectionHistory(vehicleId);
            
            log.info("Driver {} retrieved {} inspection records for vehicle {}", 
                driver.getNames(), history.size(), driver.getAssignedVehicle().getPlateNo());
            
            return ResponseEntity.ok(history);
            
        } catch (Exception e) {
            log.error("Error getting inspection history: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get latest inspection for a vehicle (ADMIN/MANAGER/RECEPTIONIST/DRIVER)
     * GET /api/vehicle-inspections/vehicle/{vehicleId}/latest
     */
    @GetMapping("/vehicle/{vehicleId}/latest")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'DRIVER')")
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
     * Get all inspections for a vehicle (ADMIN/MANAGER/RECEPTIONIST/DRIVER)
     * GET /api/vehicle-inspections/vehicle/{vehicleId}
     */
    @GetMapping("/vehicle/{vehicleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'DRIVER')")
    public ResponseEntity<List<VehicleInspectionDTO>> getVehicleInspectionHistory(@PathVariable Long vehicleId) {
        List<VehicleInspectionDTO> history = inspectionService.getVehicleInspectionHistory(vehicleId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get vehicles due for inspection soon (within 30 days)
     * GET /api/vehicle-inspections/due-soon
     */
    @GetMapping("/due-soon")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<VehicleInspectionDTO>> getVehiclesDueSoon() {
        List<VehicleInspectionDTO> dueSoon = inspectionService.getVehiclesDueSoon();
        return ResponseEntity.ok(dueSoon);
    }

    /**
     * Get overdue vehicles
     * GET /api/vehicle-inspections/overdue
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<VehicleInspectionDTO>> getOverdueVehicles() {
        List<VehicleInspectionDTO> overdue = inspectionService.getOverdueVehicles();
        return ResponseEntity.ok(overdue);
    }

    /**
     * Get dashboard summary
     * GET /api/vehicle-inspections/dashboard
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        Map<String, Object> summary = inspectionService.getDashboardSummary();
        return ResponseEntity.ok(summary);
    }
}