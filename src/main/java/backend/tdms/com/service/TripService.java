package backend.tdms.com.service;

import backend.tdms.com.dto.DailyTripResponseDTO;
import backend.tdms.com.model.DailyTrip;
import backend.tdms.com.model.Driver;
import backend.tdms.com.model.User;
import backend.tdms.com.repository.DailyTripRepository;
import backend.tdms.com.repository.DriverRepository;
import backend.tdms.com.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripService {

    private final DailyTripRepository dailyTripRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;

    /**
     * Get driver's UPCOMING trips only
     * - Trips assigned to driver's vehicle
     * - Today's trips that haven't departed yet
     * - Future trips (not yet finished)
     * - Excludes completed/cancelled trips
     */
    public List<DailyTripResponseDTO> getMyUpcomingTrips() {
        String currentUserEmail = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        
        User user = userRepository.findByEmail(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Find driver by matching phone number
        Driver driver = driverRepository.findAll().stream()
            .filter(d -> {
                boolean phoneMatch = d.getPhoneNumber() != null && 
                                   user.getPhone() != null && 
                                   d.getPhoneNumber().equals(user.getPhone());
                return phoneMatch;
            })
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Driver record not found for user: " + user.getEmail()));

        if (driver.getAssignedVehicle() == null) {
            log.warn("No vehicle assigned to driver: {}", driver.getNames());
            return List.of(); // Return empty list instead of throwing exception
        }

        // Get current date and time
        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();

        // Get upcoming trips for driver's vehicle
        List<DailyTrip> upcomingTrips = dailyTripRepository.findAll().stream()
            .filter(trip -> {
                // Must have vehicle and match driver's assigned vehicle
                if (trip.getVehicle() == null || 
                    !trip.getVehicle().getId().equals(driver.getAssignedVehicle().getId())) {
                    return false;
                }
                
                // Must have valid trip date
                if (trip.getTripDate() == null) {
                    return false;
                }
                
                // ✅ FILTER 1: Exclude past trips (before today)
                if (trip.getTripDate().isBefore(today)) {
                    return false;
                }
                
                // ✅ FILTER 2: For today's trips, exclude those that already departed
                if (trip.getTripDate().isEqual(today) && trip.getTimeSlot() != null) {
                    LocalTime departureTime = trip.getTimeSlot().getDepartureTime();
                    if (departureTime != null && departureTime.isBefore(currentTime)) {
                        return false; // Trip already departed
                    }
                }
                
                // ✅ FILTER 3: Exclude completed or cancelled trips
                String status = trip.getStatus();
                if (status != null && 
                    (status.equalsIgnoreCase("COMPLETED") || 
                     status.equalsIgnoreCase("CANCELLED"))) {
                    return false;
                }
                
                return true; // This is an upcoming trip!
            })
            .sorted((t1, t2) -> {
                // Sort by date first, then by time
                int dateCompare = t1.getTripDate().compareTo(t2.getTripDate());
                if (dateCompare != 0) return dateCompare;
                
                if (t1.getTimeSlot() != null && t2.getTimeSlot() != null &&
                    t1.getTimeSlot().getDepartureTime() != null && 
                    t2.getTimeSlot().getDepartureTime() != null) {
                    return t1.getTimeSlot().getDepartureTime()
                            .compareTo(t2.getTimeSlot().getDepartureTime());
                }
                return 0;
            })
            .collect(Collectors.toList());

        log.info("Found {} upcoming trips for driver: {} (Vehicle: {})", 
            upcomingTrips.size(), driver.getNames(), driver.getAssignedVehicle().getPlateNo());

        return upcomingTrips.stream()
            .map(this::convertTripToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Convert DailyTrip to DTO with all details
     */
    private DailyTripResponseDTO convertTripToDTO(DailyTrip trip) {
        DailyTripResponseDTO dto = new DailyTripResponseDTO();
        dto.setDailyTripId(trip.getId());
        dto.setTripDate(trip.getTripDate().toString());
        
        if (trip.getRoute() != null) {
            dto.setOrigin(trip.getRoute().getOrigin());
            dto.setDestination(trip.getRoute().getDestination());
        }
        
        if (trip.getTimeSlot() != null) {
            dto.setDepartureTime(trip.getTimeSlot().getDepartureTime().toString());
        }
        
        if (trip.getVehicle() != null) {
            dto.setVehiclePlateNo(trip.getVehicle().getPlateNo());
        }
        
        dto.setStatus(trip.getStatus());
        
        return dto;
    }
}