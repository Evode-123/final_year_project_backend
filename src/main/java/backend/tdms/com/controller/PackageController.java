package backend.tdms.com.controller;

import backend.tdms.com.dto.PackageBookingDTO;
import backend.tdms.com.dto.PackageCollectionDTO;
import backend.tdms.com.dto.PackageResponseDTO;
import backend.tdms.com.service.PackageService;
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
@RequestMapping("/api/packages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PackageController {

    private final PackageService packageService;

    /**
     * Book a new package
     * POST /api/packages/book
     */
    @PostMapping("/book")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')")
    public ResponseEntity<?> bookPackage(@RequestBody PackageBookingDTO dto) {
        try {
            log.info("Booking package from {} to {}", dto.getSenderNames(), dto.getReceiverNames());
            PackageResponseDTO pkg = packageService.bookPackage(dto);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Package booked successfully");
            response.put("trackingNumber", pkg.getTrackingNumber());
            response.put("package", pkg);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error booking package: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * ✅ Track package by tracking number (PUBLIC - no auth required)
     * Anyone with tracking number can track their package
     * GET /api/packages/track/{trackingNumber}
     */
    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<?> trackPackage(@PathVariable String trackingNumber) {
        try {
            PackageResponseDTO pkg = packageService.getPackageByTrackingNumber(trackingNumber);
            return ResponseEntity.ok(pkg);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    // ========================================
    // ✅ NEW: USER-SPECIFIC ENDPOINTS
    // ========================================

    /**
     * ✅ NEW: Get packages sent by current user
     * Uses user's phone number from profile
     * GET /api/packages/my-sent-packages
     */
    @GetMapping("/my-sent-packages")
    @PreAuthorize("hasAnyRole('OTHER_USER', 'RECEPTIONIST', 'ADMIN', 'MANAGER')")
    public ResponseEntity<List<PackageResponseDTO>> getMySentPackages() {
        log.info("Getting sent packages for current user");
        List<PackageResponseDTO> packages = packageService.getMySentPackages();
        return ResponseEntity.ok(packages);
    }

    /**
     * ✅ NEW: Get packages where current user is receiver
     * Uses user's phone number from profile
     * GET /api/packages/my-received-packages
     */
    @GetMapping("/my-received-packages")
    @PreAuthorize("hasAnyRole('OTHER_USER', 'RECEPTIONIST', 'ADMIN', 'MANAGER')")
    public ResponseEntity<List<PackageResponseDTO>> getMyReceivedPackages() {
        log.info("Getting received packages for current user");
        List<PackageResponseDTO> packages = packageService.getMyReceivedPackages();
        return ResponseEntity.ok(packages);
    }

    /**
     * ✅ NEW: Get package statistics for current user
     * GET /api/packages/my-statistics
     */
    @GetMapping("/my-statistics")
    @PreAuthorize("hasAnyRole('OTHER_USER', 'RECEPTIONIST', 'ADMIN', 'MANAGER')")
    public ResponseEntity<?> getMyPackageStatistics() {
        log.info("Getting package statistics for current user");
        Map<String, Object> stats = packageService.getMyPackageStatistics();
        return ResponseEntity.ok(stats);
    }

    // ========================================
    // EXISTING ENDPOINTS (for staff)
    // ========================================

    /**
     * Mark package as arrived
     * PUT /api/packages/{id}/arrived
     */
    @PutMapping("/{id}/arrived")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')")
    public ResponseEntity<?> markAsArrived(@PathVariable Long id) {
        try {
            PackageResponseDTO pkg = packageService.markPackageAsArrived(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Package marked as arrived. Receiver has been notified.");
            response.put("package", pkg);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error marking package as arrived: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Collect package (hand over to receiver)
     * POST /api/packages/collect
     */
    @PostMapping("/collect")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')")
    public ResponseEntity<?> collectPackage(@RequestBody PackageCollectionDTO dto) {
        try {
            log.info("Collecting package ID: {} by {}", dto.getPackageId(), dto.getCollectedByName());
            PackageResponseDTO pkg = packageService.collectPackage(dto);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Package delivered successfully. Sender has been notified.");
            response.put("package", pkg);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error collecting package: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get packages for a specific trip (STAFF ONLY)
     * GET /api/packages/trip/{tripId}
     */
    @GetMapping("/trip/{tripId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')")
    public ResponseEntity<List<PackageResponseDTO>> getPackagesForTrip(@PathVariable Long tripId) {
        List<PackageResponseDTO> packages = packageService.getPackagesForTrip(tripId);
        return ResponseEntity.ok(packages);
    }

    /**
     * Get packages by sender phone (STAFF ONLY)
     * GET /api/packages/sender/{phone}
     */
    @GetMapping("/sender/{phone}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<List<PackageResponseDTO>> getPackagesBySender(@PathVariable String phone) {
        List<PackageResponseDTO> packages = packageService.getPackagesBySender(phone);
        return ResponseEntity.ok(packages);
    }

    /**
     * Get packages by receiver phone (STAFF ONLY)
     * GET /api/packages/receiver/{phone}
     */
    @GetMapping("/receiver/{phone}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')")
    public ResponseEntity<List<PackageResponseDTO>> getPackagesByReceiver(@PathVariable String phone) {
        List<PackageResponseDTO> packages = packageService.getPackagesByReceiver(phone);
        return ResponseEntity.ok(packages);
    }

    /**
     * Get packages by status (STAFF ONLY)
     * GET /api/packages/status/{status}
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')")
    public ResponseEntity<List<PackageResponseDTO>> getPackagesByStatus(@PathVariable String status) {
        List<PackageResponseDTO> packages = packageService.getPackagesByStatus(status);
        return ResponseEntity.ok(packages);
    }

    /**
     * Get all arrived packages (STAFF ONLY)
     * GET /api/packages/arrived
     */
    @GetMapping("/arrived")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')")
    public ResponseEntity<List<PackageResponseDTO>> getArrivedPackages() {
        List<PackageResponseDTO> packages = packageService.getArrivedPackages();
        return ResponseEntity.ok(packages);
    }

    /**
     * Get all in-transit packages (STAFF ONLY)
     * GET /api/packages/in-transit
     */
    @GetMapping("/in-transit")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')")
    public ResponseEntity<List<PackageResponseDTO>> getInTransitPackages() {
        List<PackageResponseDTO> packages = packageService.getInTransitPackages();
        return ResponseEntity.ok(packages);
    }

    /**
     * Get all collected packages (STAFF ONLY)
     * GET /api/packages/collected
     */
    @GetMapping("/collected")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')")
    public ResponseEntity<List<PackageResponseDTO>> getCollectedPackages() {
        List<PackageResponseDTO> packages = packageService.getCollectedPackages();
        return ResponseEntity.ok(packages);
    }

    /**
     * Cancel package (STAFF ONLY)
     * PUT /api/packages/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')")
    public ResponseEntity<?> cancelPackage(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String reason = body.getOrDefault("reason", "Cancelled by staff");
            PackageResponseDTO pkg = packageService.cancelPackage(id, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Package cancelled successfully");
            response.put("package", pkg);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error cancelling package: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get package by ID (STAFF ONLY)
     * GET /api/packages/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')")
    public ResponseEntity<?> getPackageById(@PathVariable Long id) {
        try {
            // This would need to be implemented in service
            // For now, use tracking number
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}