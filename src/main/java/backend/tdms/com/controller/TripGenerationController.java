package backend.tdms.com.controller;

import backend.tdms.com.service.TripGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/trips")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")  // ✅ CORRECT!
public class TripGenerationController {

    private final TripGenerationService tripGenerationService;

    /**
     * ✅ NEW: Get system configuration status
     * Shows admin what's configured and what's missing
     */
    @GetMapping("/system-status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = tripGenerationService.getSystemStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * ✅ NEW: Initialize trips for first time (next 3 days)
     * Admin clicks "Initialize System" button after setting up everything
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initializeTrips() {
        Map<String, Object> response = tripGenerationService.initializeTripsForAdmin();
        
        if (!(Boolean) response.get("success")) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Generate trips for custom date range
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateTripsManually(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "1") int numberOfDays) {
        
        Map<String, Object> response = tripGenerationService.manualGenerateTrips(
            startDate, numberOfDays
        );
        
        if (!(Boolean) response.get("success")) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Generate trips for specific date
     */
    @PostMapping("/generate-for-date")
    public ResponseEntity<Map<String, Object>> generateForSpecificDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        if (!tripGenerationService.isSystemConfigured()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "System not configured",
                "errors", tripGenerationService.getSystemStatus().get("warnings")
            ));
        }
        
        int generated = tripGenerationService.generateTripsForDate(date);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Trips generated for " + date,
            "date", date.toString(),
            "tripsGenerated", generated
        ));
    }

    /**
     * Trigger the midnight job manually (generates for 2 days from now)
     */
    @PostMapping("/generate-now")
    public ResponseEntity<Map<String, Object>> triggerImmediateGeneration() {
        if (!tripGenerationService.isSystemConfigured()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "System not configured",
                "errors", tripGenerationService.getSystemStatus().get("warnings")
            ));
        }
        
        tripGenerationService.generateDailyTrips();
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Immediate trip generation triggered",
            "note", "This generates trips for 2 days from now"
        ));
    }
}