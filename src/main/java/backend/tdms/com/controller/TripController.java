package backend.tdms.com.controller;

import backend.tdms.com.dto.DailyTripResponseDTO;
import backend.tdms.com.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    /**
     * Get upcoming trips for the current driver
     * Only shows trips that haven't departed yet
     */
    @GetMapping("/my-upcoming")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<DailyTripResponseDTO>> getMyUpcomingTrips() {
        log.info("Getting upcoming trips for current driver");
        List<DailyTripResponseDTO> trips = tripService.getMyUpcomingTrips();
        return ResponseEntity.ok(trips);
    }
}