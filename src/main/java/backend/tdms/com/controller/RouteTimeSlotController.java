package backend.tdms.com.controller;

import backend.tdms.com.model.RouteTimeSlot;
import backend.tdms.com.service.RouteTimeSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/route-timeslots")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class RouteTimeSlotController {

    private final RouteTimeSlotService routeTimeSlotService;

    @PostMapping("/assign")
    public ResponseEntity<RouteTimeSlot> assignTimeSlotToRoute(
            @RequestParam Long routeId,
            @RequestParam Long timeSlotId) {
        RouteTimeSlot routeTimeSlot = routeTimeSlotService.assignTimeSlotToRoute(routeId, timeSlotId);
        return new ResponseEntity<>(routeTimeSlot, HttpStatus.CREATED);
    }

    @GetMapping("/route/{routeId}")
    public ResponseEntity<List<RouteTimeSlot>> getTimeSlotsForRoute(@PathVariable Long routeId) {
        List<RouteTimeSlot> routeTimeSlots = routeTimeSlotService.getTimeSlotsForRoute(routeId);
        return ResponseEntity.ok(routeTimeSlots);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeTimeSlotFromRoute(@PathVariable Long id) {
        routeTimeSlotService.removeTimeSlotFromRoute(id);
        return ResponseEntity.noContent().build();
    }
}