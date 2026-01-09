package backend.tdms.com.controller;

import backend.tdms.com.dto.TimeSlotDTO;
import backend.tdms.com.model.TimeSlot;
import backend.tdms.com.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/timeslots")
@RequiredArgsConstructor
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TimeSlot> createTimeSlot(@RequestBody TimeSlotDTO dto) {
        TimeSlot timeSlot = timeSlotService.createTimeSlot(dto);
        return new ResponseEntity<>(timeSlot, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TimeSlot>> getAllTimeSlots() {
        List<TimeSlot> timeSlots = timeSlotService.getAllActiveTimeSlots();
        return ResponseEntity.ok(timeSlots);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TimeSlot> getTimeSlotById(@PathVariable Long id) {
        TimeSlot timeSlot = timeSlotService.getTimeSlotById(id);
        return ResponseEntity.ok(timeSlot);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TimeSlot> updateTimeSlot(
            @PathVariable Long id,
            @RequestBody TimeSlotDTO dto) {
        TimeSlot timeSlot = timeSlotService.updateTimeSlot(id, dto);
        return ResponseEntity.ok(timeSlot);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTimeSlot(@PathVariable Long id) {
        timeSlotService.deleteTimeSlot(id);
        return ResponseEntity.noContent().build();
    }
}