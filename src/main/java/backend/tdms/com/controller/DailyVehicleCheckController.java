package backend.tdms.com.controller;

import backend.tdms.com.dto.DailyCheckResponseDTO;
import backend.tdms.com.dto.DriverCheckDTO;
import backend.tdms.com.model.Driver;
import backend.tdms.com.model.User;
import backend.tdms.com.repository.DriverRepository;
import backend.tdms.com.repository.UserRepository;
import backend.tdms.com.service.DailyVehicleCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/daily-checks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DailyVehicleCheckController {

    private final DailyVehicleCheckService checkService;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;

    /**
     * Driver submits daily check
     * POST /api/daily-checks/submit
     */
    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('ADMIN','DRIVER', 'MANAGER')")
    public ResponseEntity<?> submitCheck(@RequestBody DriverCheckDTO dto) {
        try {
            log.info("Submitting daily check for vehicle {}", dto.getVehicleId());
            DailyCheckResponseDTO check = checkService.submitCheck(dto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Daily check submitted successfully");
            response.put("check", check);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error submitting check: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * ✅ NEW: Get current driver's vehicle daily check history
     * GET /api/daily-checks/my-vehicle/history?days=30
     */
    @GetMapping("/my-vehicle/history")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<?> getMyVehicleCheckHistory(
        @RequestParam(defaultValue = "30") int days
    ) {
        try {
            String currentUserEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();
            
            log.info("Driver {} requesting daily check history (last {} days)", currentUserEmail, days);
            
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
                log.warn("Driver {} has no assigned vehicle", driver.getNames());
                return ResponseEntity.ok(Collections.emptyList());
            }
            
            // Get check history
            Long vehicleId = driver.getAssignedVehicle().getId();
            List<DailyCheckResponseDTO> checks = checkService.getVehicleCheckHistory(vehicleId, days);
            
            log.info("Driver {} retrieved {} daily checks for vehicle {}", 
                driver.getNames(), checks.size(), driver.getAssignedVehicle().getPlateNo());
            
            return ResponseEntity.ok(checks);
            
        } catch (Exception e) {
            log.error("Error getting daily check history: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * ✅ NEW: Get latest check for driver's assigned vehicle
     * GET /api/daily-checks/my-vehicle/latest
     */
    @GetMapping("/my-vehicle/latest")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<?> getMyVehicleLatestCheck() {
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
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "No vehicle assigned to you");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            // Get latest check
            Long vehicleId = driver.getAssignedVehicle().getId();
            DailyCheckResponseDTO check = checkService.getLatestCheck(vehicleId);
            
            return ResponseEntity.ok(check);
            
        } catch (RuntimeException e) {
            // No checks found
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            log.error("Error getting latest check: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * ✅ NEW: Get driver's vehicle information with latest check status
     * GET /api/daily-checks/my-vehicle
     */
    @GetMapping("/my-vehicle")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<?> getMyVehicleInfo() {
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
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "No vehicle assigned to you");
                response.put("hasVehicle", false);
                return ResponseEntity.ok(response);
            }
            
            // Get vehicle info
            Long vehicleId = driver.getAssignedVehicle().getId();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hasVehicle", true);
            response.put("vehicle", Map.of(
                "id", driver.getAssignedVehicle().getId(),
                "plateNo", driver.getAssignedVehicle().getPlateNo(),
                "vehicleType", driver.getAssignedVehicle().getVehicleType(),
                "capacity", driver.getAssignedVehicle().getCapacity()
            ));
            
            // Try to get latest check
            try {
                DailyCheckResponseDTO latestCheck = checkService.getLatestCheck(vehicleId);
                response.put("latestCheck", latestCheck);
            } catch (Exception e) {
                response.put("latestCheck", null);
                response.put("message", "No daily checks found for this vehicle");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting vehicle info: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get today's checks (ADMIN/MANAGER/DRIVER)
     * GET /api/daily-checks/today
     */
    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DRIVER')")
    public ResponseEntity<List<DailyCheckResponseDTO>> getTodaysChecks() {
        List<DailyCheckResponseDTO> checks = checkService.getTodaysChecks();
        return ResponseEntity.ok(checks);
    }

    /**
     * Get checks with problems
     * GET /api/daily-checks/problems
     */
    @GetMapping("/problems")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DRIVER')")
    public ResponseEntity<List<DailyCheckResponseDTO>> getChecksWithProblems() {
        List<DailyCheckResponseDTO> checks = checkService.getChecksWithProblems();
        return ResponseEntity.ok(checks);
    }

    /**
     * Get unreviewed problems
     * GET /api/daily-checks/unreviewed
     */
    @GetMapping("/unreviewed")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DRIVER')")
    public ResponseEntity<List<DailyCheckResponseDTO>> getUnreviewedProblems() {
        List<DailyCheckResponseDTO> checks = checkService.getUnreviewedProblems();
        return ResponseEntity.ok(checks);
    }

    /**
     * Get urgent checks
     * GET /api/daily-checks/urgent
     */
    @GetMapping("/urgent")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DRIVER')")
    public ResponseEntity<List<DailyCheckResponseDTO>> getUrgentChecks() {
        List<DailyCheckResponseDTO> checks = checkService.getUrgentChecks();
        return ResponseEntity.ok(checks);
    }

    /**
     * Get latest check for vehicle
     * GET /api/daily-checks/vehicle/{vehicleId}/latest
     */
    @GetMapping("/vehicle/{vehicleId}/latest")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DRIVER')")
    public ResponseEntity<?> getLatestCheck(@PathVariable Long vehicleId) {
        try {
            DailyCheckResponseDTO check = checkService.getLatestCheck(vehicleId);
            return ResponseEntity.ok(check);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Get check history for vehicle
     * GET /api/daily-checks/vehicle/{vehicleId}/history?days=30
     */
    @GetMapping("/vehicle/{vehicleId}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DRIVER')")
    public ResponseEntity<List<DailyCheckResponseDTO>> getVehicleCheckHistory(
        @PathVariable Long vehicleId,
        @RequestParam(defaultValue = "30") int days
    ) {
        List<DailyCheckResponseDTO> checks = checkService.getVehicleCheckHistory(vehicleId, days);
        return ResponseEntity.ok(checks);
    }

    /**
     * Manager reviews a check
     * PUT /api/daily-checks/{checkId}/review
     */
    @PutMapping("/{checkId}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DRIVER')")
    public ResponseEntity<?> reviewCheck(
        @PathVariable Long checkId,
        @RequestBody Map<String, String> body
    ) {
        try {
            String managerNotes = body.get("managerNotes");
            String actionTaken = body.get("actionTaken");

            DailyCheckResponseDTO check = checkService.reviewCheck(checkId, managerNotes, actionTaken);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Check reviewed successfully");
            response.put("check", check);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error reviewing check: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get dashboard summary
     * GET /api/daily-checks/dashboard
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DRIVER')")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        Map<String, Object> summary = checkService.getDashboardSummary();
        return ResponseEntity.ok(summary);
    }
}