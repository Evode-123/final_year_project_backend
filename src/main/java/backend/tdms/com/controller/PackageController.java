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
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')") // ✅ Correct syntax
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
     * Track package by tracking number (PUBLIC)
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

    /**
     * Mark package as arrived
     * PUT /api/packages/{id}/arrived
     */
    @PutMapping("/{id}/arrived")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')") // ✅ Correct syntax
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
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')") // ✅ Correct syntax
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
     * Get packages for a specific trip
     * GET /api/packages/trip/{tripId}
     */
    @GetMapping("/trip/{tripId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')") // ✅ Added MANAGER
    public ResponseEntity<List<PackageResponseDTO>> getPackagesForTrip(@PathVariable Long tripId) {
        List<PackageResponseDTO> packages = packageService.getPackagesForTrip(tripId);
        return ResponseEntity.ok(packages);
    }

    /**
     * Get packages by sender phone
     * GET /api/packages/sender/{phone}
     */
    @GetMapping("/sender/{phone}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')") // ✅ Added RECEPTIONIST
    public ResponseEntity<List<PackageResponseDTO>> getPackagesBySender(@PathVariable String phone) {
        List<PackageResponseDTO> packages = packageService.getPackagesBySender(phone);
        return ResponseEntity.ok(packages);
    }

    /**
     * Get packages by receiver phone
     * GET /api/packages/receiver/{phone}
     */
    @GetMapping("/receiver/{phone}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')") // ✅ Added MANAGER
    public ResponseEntity<List<PackageResponseDTO>> getPackagesByReceiver(@PathVariable String phone) {
        List<PackageResponseDTO> packages = packageService.getPackagesByReceiver(phone);
        return ResponseEntity.ok(packages);
    }

    /**
     * Get packages by status
     * GET /api/packages/status/{status}
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')") // ✅ Added MANAGER
    public ResponseEntity<List<PackageResponseDTO>> getPackagesByStatus(@PathVariable String status) {
        List<PackageResponseDTO> packages = packageService.getPackagesByStatus(status);
        return ResponseEntity.ok(packages);
    }

    /**
     * Get all arrived packages (ready for collection)
     * GET /api/packages/arrived
     */
    @GetMapping("/arrived")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')") // ✅ Added MANAGER
    public ResponseEntity<List<PackageResponseDTO>> getArrivedPackages() {
        List<PackageResponseDTO> packages = packageService.getArrivedPackages();
        return ResponseEntity.ok(packages);
    }

    /**
     * Get all in-transit packages
     * GET /api/packages/in-transit
     */
    @GetMapping("/in-transit")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')") // ✅ Added MANAGER
    public ResponseEntity<List<PackageResponseDTO>> getInTransitPackages() {
        List<PackageResponseDTO> packages = packageService.getInTransitPackages();
        return ResponseEntity.ok(packages);
    }

    /**
     * Get all collected packages
     * GET /api/packages/collected
     */
    @GetMapping("/collected")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')") // ✅ Correct syntax
    public ResponseEntity<List<PackageResponseDTO>> getCollectedPackages() {
        List<PackageResponseDTO> packages = packageService.getCollectedPackages();
        return ResponseEntity.ok(packages);
    }

    /**
     * Cancel package
     * PUT /api/packages/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')") // ✅ Correct syntax
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
     * Get package by ID
     * GET /api/packages/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'MANAGER')") // ✅ Added MANAGER
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