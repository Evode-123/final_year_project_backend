package backend.tdms.com.controller;

import backend.tdms.com.dto.RecordInspectionDTO;
import backend.tdms.com.dto.VehicleInspectionDTO;
import backend.tdms.com.service.VehicleInspectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    /**
     * Record a new inspection
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
     * Get latest inspection for a vehicle
     * GET /api/vehicle-inspections/vehicle/{vehicleId}/latest
     */
    @GetMapping("/vehicle/{vehicleId}/latest")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
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
     * Get all inspections for a vehicle
     * GET /api/vehicle-inspections/vehicle/{vehicleId}
     */
    @GetMapping("/vehicle/{vehicleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
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