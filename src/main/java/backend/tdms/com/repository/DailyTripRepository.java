package backend.tdms.com.repository;

import backend.tdms.com.model.DailyTrip;
import backend.tdms.com.model.Route;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyTripRepository extends JpaRepository<DailyTrip, Long> {

    // ✅ Pessimistic locking for bookings with eager loading
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT dt FROM DailyTrip dt " +
           "JOIN FETCH dt.route " +
           "JOIN FETCH dt.timeSlot " +
           "JOIN FETCH dt.vehicle " +
           "WHERE dt.id = :id")
    Optional<DailyTrip> findByIdWithLock(@Param("id") Long id);
    
    // ✅ Count trips for a specific date
    @Query("SELECT COUNT(dt) FROM DailyTrip dt WHERE dt.tripDate = :tripDate")
    long countByTripDate(@Param("tripDate") LocalDate tripDate);
    
    // ✅ Find by trip date with eager loading
    @Query("SELECT dt FROM DailyTrip dt " +
           "JOIN FETCH dt.route " +
           "JOIN FETCH dt.timeSlot " +
           "JOIN FETCH dt.vehicle " +
           "WHERE dt.tripDate = :tripDate")
    List<DailyTrip> findByTripDate(@Param("tripDate") LocalDate tripDate);

    // Add this method to DailyTripRepository.java
    @Query("SELECT dt FROM DailyTrip dt " +
       "WHERE dt.tripDate >= :startDate " +
       "AND dt.tripDate <= :endDate " +
       "AND dt.availableSeats > 0 " +
       "AND dt.status = 'SCHEDULED' " +
       "AND (dt.tripDate > :today OR " +
       "(dt.tripDate = :today AND dt.timeSlot.departureTime > :currentTime)) " +
       "ORDER BY dt.tripDate, dt.timeSlot.departureTime")
    List<DailyTrip> findAvailableTripsExcludingPast(
       @Param("today") LocalDate today,
       @Param("currentTime") LocalTime currentTime,
       @Param("startDate") LocalDate startDate,
       @Param("endDate") LocalDate endDate
);

@Query("SELECT dt FROM DailyTrip dt " +
       "WHERE dt.route.origin = :origin " +
       "AND dt.route.destination = :destination " +
       "AND dt.tripDate = :tripDate " +
       "AND dt.availableSeats > 0 " +
       "AND dt.status = 'SCHEDULED' " +
       "AND (dt.tripDate > :today OR " +
       "(dt.tripDate = :today AND dt.timeSlot.departureTime > :currentTime)) " +
       "ORDER BY dt.timeSlot.departureTime")
List<DailyTrip> findAvailableTripsByRouteAndDateExcludingPast(
    @Param("origin") String origin,
    @Param("destination") String destination,
    @Param("tripDate") LocalDate tripDate,
    @Param("today") LocalDate today,
    @Param("currentTime") LocalTime currentTime
);
    
    // ✅ Find by trip date and status with eager loading
    @Query("SELECT dt FROM DailyTrip dt " +
           "JOIN FETCH dt.route " +
           "JOIN FETCH dt.timeSlot " +
           "JOIN FETCH dt.vehicle " +
           "WHERE dt.tripDate = :tripDate AND dt.status = :status")
    List<DailyTrip> findByTripDateAndStatus(
            @Param("tripDate") LocalDate tripDate, 
            @Param("status") String status);
    
    // ✅ Find by date, route and time with eager loading
    @Query("SELECT dt FROM DailyTrip dt " +
           "JOIN FETCH dt.route r " +
           "JOIN FETCH dt.timeSlot ts " +
           "JOIN FETCH dt.vehicle " +
           "WHERE dt.tripDate = :tripDate " +
           "AND r.id = :routeId " +
           "AND ts.departureTime = :departureTime")
    Optional<DailyTrip> findByDateAndRouteAndTime(
            @Param("tripDate") LocalDate tripDate, 
            @Param("routeId") Long routeId, 
            @Param("departureTime") LocalTime departureTime);
    
    // ✅ CRITICAL FIX: Find available trips with all relationships loaded
    @Query("SELECT dt FROM DailyTrip dt " +
           "JOIN FETCH dt.route r " +
           "JOIN FETCH dt.timeSlot ts " +
           "JOIN FETCH dt.vehicle v " +
           "WHERE dt.tripDate >= :startDate " +
           "AND dt.tripDate <= :endDate " +
           "AND dt.status = 'SCHEDULED' " +
           "AND dt.availableSeats > 0 " +
           "ORDER BY dt.tripDate, ts.departureTime")
    List<DailyTrip> findAvailableTrips(
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate);
    
    // ✅ Find by date and vehicle with eager loading
    @Query("SELECT dt FROM DailyTrip dt " +
           "JOIN FETCH dt.route " +
           "JOIN FETCH dt.timeSlot " +
           "JOIN FETCH dt.vehicle v " +
           "WHERE dt.tripDate = :tripDate " +
           "AND v.id = :vehicleId")
    List<DailyTrip> findByDateAndVehicle(
            @Param("tripDate") LocalDate tripDate, 
            @Param("vehicleId") Long vehicleId);
    
    // ✅ CRITICAL FIX: Search trips by route and date with all relationships
    @Query("SELECT dt FROM DailyTrip dt " +
           "JOIN FETCH dt.route r " +
           "JOIN FETCH dt.timeSlot ts " +
           "JOIN FETCH dt.vehicle v " +
           "WHERE r.origin = :origin " +
           "AND r.destination = :destination " +
           "AND dt.tripDate = :tripDate " +
           "AND dt.availableSeats > 0 " +
           "AND dt.status = 'SCHEDULED' " +
           "ORDER BY ts.departureTime")
    List<DailyTrip> findAvailableTripsByRouteAndDate(
            @Param("origin") String origin, 
            @Param("destination") String destination, 
            @Param("tripDate") LocalDate tripDate);
    
    boolean existsByTripDateAndRouteAndTimeSlot(
            LocalDate tripDate, 
            Route route, 
            backend.tdms.com.model.TimeSlot timeSlot);
}