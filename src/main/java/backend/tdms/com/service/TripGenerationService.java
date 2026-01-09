package backend.tdms.com.service;

import backend.tdms.com.model.*;
import backend.tdms.com.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripGenerationService {

    private final DailyTripRepository dailyTripRepository;
    private final RouteTimeSlotRepository routeTimeSlotRepository;
    private final RouteVehicleRepository routeVehicleRepository;
    private final RouteRepository routeRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final VehicleRepository vehicleRepository;

    /**
     * ✅ REMOVED AUTOMATIC STARTUP GENERATION
     * System will NOT generate trips automatically on startup
     * Admin must manually trigger trip generation after setting up:
     * 1. Routes
     * 2. Time Slots
     * 3. Vehicles
     * 4. Route-TimeSlot assignments
     * 5. Route-Vehicle assignments
     */

    /**
     * Scheduled automatic generation - runs every day at midnight (00:00)
     * Only runs if system has been initialized by admin
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void generateDailyTrips() {
        // Check if system is configured before auto-generating
        if (!isSystemConfigured()) {
            log.warn("⚠️  Skipping automatic trip generation - System not configured yet");
            log.warn("   Please configure routes, time slots, vehicles and their assignments");
            return;
        }

        log.info("Starting scheduled automatic trip generation...");
        
        // Generate trips for 2 days from now
        LocalDate targetDate = LocalDate.now().plusDays(2);
        
        generateTripsForDate(targetDate);
        
        log.info("Scheduled trip generation completed for date: {}", targetDate);
    }

    /**
     * Check if system has minimum required configuration
     */
    public boolean isSystemConfigured() {
        long routes = routeRepository.count();
        long timeSlots = timeSlotRepository.count();
        long vehicles = vehicleRepository.count();
        long routeTimeSlots = routeTimeSlotRepository.count();
        long routeVehicles = routeVehicleRepository.count();

        return routes > 0 && timeSlots > 0 && vehicles > 0 
               && routeTimeSlots > 0 && routeVehicles > 0;
    }

    /**
     * Get system configuration status for admin dashboard
     */
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        long routes = routeRepository.count();
        long timeSlots = timeSlotRepository.count();
        long vehicles = vehicleRepository.count();
        long routeTimeSlots = routeTimeSlotRepository.count();
        long routeVehicles = routeVehicleRepository.count();
        
        LocalDate today = LocalDate.now();
        long todayTrips = dailyTripRepository.countByTripDate(today);
        long tomorrowTrips = dailyTripRepository.countByTripDate(today.plusDays(1));
        long dayAfterTrips = dailyTripRepository.countByTripDate(today.plusDays(2));
        
        status.put("routes", routes);
        status.put("timeSlots", timeSlots);
        status.put("vehicles", vehicles);
        status.put("routeTimeSlotAssignments", routeTimeSlots);
        status.put("routeVehicleAssignments", routeVehicles);
        status.put("isConfigured", isSystemConfigured());
        
        status.put("tripsToday", todayTrips);
        status.put("tripsTomorrow", tomorrowTrips);
        status.put("tripsDayAfter", dayAfterTrips);
        status.put("totalUpcomingTrips", todayTrips + tomorrowTrips + dayAfterTrips);
        
        // Configuration warnings
        List<String> warnings = new ArrayList<>();
        if (routes == 0) warnings.add("No routes configured");
        if (timeSlots == 0) warnings.add("No time slots configured");
        if (vehicles == 0) warnings.add("No vehicles configured");
        if (routeTimeSlots == 0) warnings.add("No time slots assigned to routes");
        if (routeVehicles == 0) warnings.add("No vehicles assigned to routes");
        
        status.put("warnings", warnings);
        status.put("canGenerateTrips", warnings.isEmpty());
        
        return status;
    }

    /**
     * Admin-triggered: Initialize trips for the next 3 days
     * This should be called after admin sets up all routes, vehicles, etc.
     */
    @Transactional
    public Map<String, Object> initializeTripsForAdmin() {
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║   ADMIN TRIP INITIALIZATION - Starting                    ║");
        log.info("╚════════════════════════════════════════════════════════════╝");

        // Validate system configuration
        if (!isSystemConfigured()) {
            log.error("❌ Cannot initialize trips - System not properly configured");
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "System not configured properly");
            errorResponse.put("errors", getConfigurationErrors());
            return errorResponse;
        }

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfterTomorrow = today.plusDays(2);
        
        Map<String, Integer> generationResults = new HashMap<>();
        
        // Generate for today
        log.info("Generating trips for today ({})...", today);
        int todayGenerated = generateTripsForDate(today);
        generationResults.put("today", todayGenerated);
        
        // Generate for tomorrow
        log.info("Generating trips for tomorrow ({})...", tomorrow);
        int tomorrowGenerated = generateTripsForDate(tomorrow);
        generationResults.put("tomorrow", tomorrowGenerated);
        
        // Generate for day after tomorrow
        log.info("Generating trips for day after tomorrow ({})...", dayAfterTomorrow);
        int dayAfterGenerated = generateTripsForDate(dayAfterTomorrow);
        generationResults.put("dayAfterTomorrow", dayAfterGenerated);
        
        int totalGenerated = todayGenerated + tomorrowGenerated + dayAfterGenerated;
        
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║   ✅ TRIP INITIALIZATION COMPLETED                         ║");
        log.info("║   Total trips generated: {}                               ║", 
            String.format("%-33s", totalGenerated));
        log.info("╚════════════════════════════════════════════════════════════╝");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Trips initialized successfully");
        response.put("totalTripsGenerated", totalGenerated);
        response.put("breakdown", generationResults);
        response.put("dates", Map.of(
            "today", today.toString(),
            "tomorrow", tomorrow.toString(),
            "dayAfterTomorrow", dayAfterTomorrow.toString()
        ));
        
        return response;
    }

    /**
     * Get configuration errors/warnings
     */
    private List<String> getConfigurationErrors() {
        List<String> errors = new ArrayList<>();
        
        if (routeRepository.count() == 0) {
            errors.add("No routes configured. Please create at least one route.");
        }
        if (timeSlotRepository.count() == 0) {
            errors.add("No time slots configured. Please create at least one time slot.");
        }
        if (vehicleRepository.count() == 0) {
            errors.add("No vehicles configured. Please create at least one vehicle.");
        }
        if (routeTimeSlotRepository.count() == 0) {
            errors.add("No time slots assigned to routes. Please assign time slots to routes.");
        }
        if (routeVehicleRepository.count() == 0) {
            errors.add("No vehicles assigned to routes. Please assign vehicles to routes.");
        }
        
        return errors;
    }

    /**
     * Generate trips for a specific date
     * Returns number of trips generated
     */
    @Transactional
    public int generateTripsForDate(LocalDate targetDate) {
        log.info("Generating trips for date: {}", targetDate);

        List<RouteTimeSlot> routeTimeSlots = routeTimeSlotRepository.findByIsActiveTrue();

        if (routeTimeSlots.isEmpty()) {
            log.warn("⚠️ No active route-timeslot combinations found!");
            return 0;
        }

        int generatedCount = 0;
        int skippedCount = 0;

        for (RouteTimeSlot routeTimeSlot : routeTimeSlots) {
            Route route = routeTimeSlot.getRoute();
            TimeSlot timeSlot = routeTimeSlot.getTimeSlot();

            // Check if trip already exists
            if (dailyTripRepository.existsByTripDateAndRouteAndTimeSlot(
                targetDate, route, timeSlot)) {
                log.debug("Trip already exists for route {} at time {}", 
                    route.getOrigin() + " → " + route.getDestination(), 
                    timeSlot.getDepartureTime());
                skippedCount++;
                continue;
            }

            // Get vehicles assigned to this route
            List<Vehicle> assignedVehicles = routeVehicleRepository
                .findVehiclesByRouteId(route.getId());

            if (assignedVehicles.isEmpty()) {
                log.warn("No vehicles assigned to route: {} → {}", 
                    route.getOrigin(), route.getDestination());
                continue;
            }

            // Find best available vehicle
            Vehicle selectedVehicle = selectBestVehicleForTrip(
                assignedVehicles, 
                targetDate, 
                timeSlot.getDepartureTime(),
                route
            );

            if (selectedVehicle == null) {
                log.warn("No available vehicle found for route {} at time {}", 
                    route.getOrigin() + " → " + route.getDestination(), 
                    timeSlot.getDepartureTime());
                continue;
            }

            // Create the daily trip
            DailyTrip dailyTrip = new DailyTrip();
            dailyTrip.setRoute(route);
            dailyTrip.setTimeSlot(timeSlot);
            dailyTrip.setVehicle(selectedVehicle);
            dailyTrip.setTripDate(targetDate);
            dailyTrip.setAvailableSeats(selectedVehicle.getCapacity());
            dailyTrip.setCurrentLocation("ORIGIN");
            dailyTrip.setStatus("SCHEDULED");

            dailyTripRepository.save(dailyTrip);
            generatedCount++;

            log.debug("Generated trip: {} → {} at {} using vehicle {}", 
                route.getOrigin(), 
                route.getDestination(), 
                timeSlot.getDepartureTime(), 
                selectedVehicle.getPlateNo());
        }

        log.info("Trip generation summary for {} - Generated: {}, Skipped: {}", 
            targetDate, generatedCount, skippedCount);
        
        return generatedCount;
    }

    /**
     * Smart vehicle selection based on availability and location
     */
    private Vehicle selectBestVehicleForTrip(List<Vehicle> assignedVehicles, 
                                            LocalDate tripDate, 
                                            LocalTime departureTime,
                                            Route route) {
        
        for (Vehicle vehicle : assignedVehicles) {
            // Check vehicle status
            if (!"AVAILABLE".equals(vehicle.getStatus()) || !vehicle.getIsActive()) {
                continue;
            }

            // Check if vehicle is available at this time
            if (isVehicleAvailable(vehicle, tripDate, departureTime, route)) {
                return vehicle;
            }
        }

        return null; // No available vehicle found
    }

    /**
     * Check if vehicle is available considering time and location conflicts
     */
    private boolean isVehicleAvailable(Vehicle vehicle, LocalDate tripDate, 
                                      LocalTime departureTime, Route route) {
        
        // Get all trips for this vehicle on this date
        List<DailyTrip> vehicleTrips = dailyTripRepository
            .findByDateAndVehicle(tripDate, vehicle.getId());

        for (DailyTrip existingTrip : vehicleTrips) {
            LocalTime existingDeparture = existingTrip.getTimeSlot().getDepartureTime();
            Route existingRoute = existingTrip.getRoute();

            // Calculate when this trip ends (departure time + duration + buffer)
            int durationMinutes = existingRoute.getDurationMinutes() != null ? 
                                 existingRoute.getDurationMinutes() : 120;
            
            // Use route-specific buffer
            int bufferMinutes = existingRoute.getTurnaroundBufferMinutes() != null ?
                               existingRoute.getTurnaroundBufferMinutes() : 30;
            
            LocalTime existingTripEndTime = existingDeparture
                .plusMinutes(durationMinutes)
                .plusMinutes(bufferMinutes);

            // Check if there's a time conflict
            if (departureTime.isBefore(existingTripEndTime)) {
                // Check if vehicle is at the right location
                String existingDestination = existingRoute.getDestination();
                String newOrigin = route.getOrigin();

                // If previous trip ends at different location than new trip starts
                if (!existingDestination.equals(newOrigin)) {
                    log.debug("Vehicle {} location conflict: at {} but needs to be at {}", 
                        vehicle.getPlateNo(), existingDestination, newOrigin);
                    return false; // Vehicle can't be at two places
                }
            }
        }

        return true; // Vehicle is available
    }

    /**
     * Manual trip generation (for admin use)
     */
    @Transactional
    public Map<String, Object> manualGenerateTrips(LocalDate startDate, int numberOfDays) {
        log.info("Manual trip generation requested for {} days starting from {}", 
            numberOfDays, startDate);
        
        if (!isSystemConfigured()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "System not configured properly");
            errorResponse.put("errors", getConfigurationErrors());
            return errorResponse;
        }
        
        Map<String, Integer> dailyResults = new LinkedHashMap<>();
        int totalGenerated = 0;
        
        for (int i = 0; i < numberOfDays; i++) {
            LocalDate targetDate = startDate.plusDays(i);
            int generated = generateTripsForDate(targetDate);
            dailyResults.put(targetDate.toString(), generated);
            totalGenerated += generated;
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Trips generated successfully");
        response.put("totalTripsGenerated", totalGenerated);
        response.put("dailyBreakdown", dailyResults);
        response.put("startDate", startDate.toString());
        response.put("numberOfDays", numberOfDays);
        
        return response;
    }
}