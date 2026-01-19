package backend.tdms.com.controller;

import backend.tdms.com.dto.*;
import backend.tdms.com.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    /**
     * Get driver's scheduled trips for incident reporting
     * Returns trips assigned to driver's vehicle (today and future)
     */
    @GetMapping("/my-trips")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<List<DailyTripResponseDTO>> getMyScheduledTrips() {
        List<DailyTripResponseDTO> trips = incidentService.getDriverScheduledTrips();
        return ResponseEntity.ok(trips);
    }

    /**
     * Report new incident (Driver can report)
     */
    @PostMapping("/report")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<IncidentResponseDTO> reportIncident(
            @RequestBody IncidentReportDTO dto) {
        IncidentResponseDTO incident = incidentService.reportIncident(dto);
        return new ResponseEntity<>(incident, HttpStatus.CREATED);
    }

    /**
     * Update incident status (Admin/Manager only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<IncidentResponseDTO> updateIncident(
            @PathVariable Long id,
            @RequestBody IncidentUpdateDTO dto) {
        IncidentResponseDTO incident = incidentService.updateIncident(id, dto);
        return ResponseEntity.ok(incident);
    }

    /**
     * Mark incident as viewed by reporter (clears badge)
     */
    @PutMapping("/{id}/mark-viewed")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<Void> markIncidentAsViewed(@PathVariable Long id) {
        incidentService.markIncidentAsViewed(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Get count of unviewed incident updates for current user
     */
    @GetMapping("/unviewed-count")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Long>> getUnviewedCount() {
        Long count = incidentService.getUnviewedIncidentsCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Get all incidents (Admin/Manager)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<IncidentResponseDTO>> getAllIncidents() {
        List<IncidentResponseDTO> incidents = incidentService.getAllIncidents();
        return ResponseEntity.ok(incidents);
    }

    /**
     * Get unresolved incidents (Admin/Manager)
     */
    @GetMapping("/unresolved")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<IncidentResponseDTO>> getUnresolvedIncidents() {
        List<IncidentResponseDTO> incidents = incidentService.getUnresolvedIncidents();
        return ResponseEntity.ok(incidents);
    }

    /**
     * Get critical incidents (Admin/Manager)
     */
    @GetMapping("/critical")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<IncidentResponseDTO>> getCriticalIncidents() {
        List<IncidentResponseDTO> incidents = incidentService.getCriticalIncidents();
        return ResponseEntity.ok(incidents);
    }

    /**
     * Get today's incidents (Admin/Manager)
     */
    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<IncidentResponseDTO>> getTodayIncidents() {
        List<IncidentResponseDTO> incidents = incidentService.getTodayIncidents();
        return ResponseEntity.ok(incidents);
    }

    /**
     * Get incidents by trip (Admin/Manager/Driver)
     */
    @GetMapping("/trip/{dailyTripId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DRIVER')")
    public ResponseEntity<List<IncidentResponseDTO>> getIncidentsByTrip(
            @PathVariable Long dailyTripId) {
        List<IncidentResponseDTO> incidents = incidentService.getIncidentsByTrip(dailyTripId);
        return ResponseEntity.ok(incidents);
    }

    /**
     * Get ALL my incidents (incidents reported by current user)
     */
    @GetMapping("/my-incidents")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<List<IncidentResponseDTO>> getMyIncidents() {
        List<IncidentResponseDTO> incidents = incidentService.getMyIncidents();
        return ResponseEntity.ok(incidents);
    }

    /**
     * ✅ NEW: Get my RESOLVED incidents
     */
    @GetMapping("/my-incidents/resolved")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<List<IncidentResponseDTO>> getMyResolvedIncidents() {
        List<IncidentResponseDTO> incidents = incidentService.getMyResolvedIncidents();
        return ResponseEntity.ok(incidents);
    }

    /**
     * ✅ NEW: Get my PENDING (unresolved) incidents
     */
    @GetMapping("/my-incidents/pending")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<List<IncidentResponseDTO>> getMyPendingIncidents() {
        List<IncidentResponseDTO> incidents = incidentService.getMyPendingIncidents();
        return ResponseEntity.ok(incidents);
    }

    /**
     * Get incident by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<IncidentResponseDTO> getIncidentById(@PathVariable Long id) {
        IncidentResponseDTO incident = incidentService.getIncidentById(id);
        return ResponseEntity.ok(incident);
    }

    /**
     * Get incident statistics (Admin/Manager only)
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<IncidentStatisticsDTO> getStatistics() {
        IncidentStatisticsDTO stats = incidentService.getStatistics();
        return ResponseEntity.ok(stats);
    }
}