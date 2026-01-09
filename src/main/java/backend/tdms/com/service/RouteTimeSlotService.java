package backend.tdms.com.service;

import backend.tdms.com.model.Route;
import backend.tdms.com.model.RouteTimeSlot;
import backend.tdms.com.model.TimeSlot;
import backend.tdms.com.repository.RouteRepository;
import backend.tdms.com.repository.RouteTimeSlotRepository;
import backend.tdms.com.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteTimeSlotService {

    private final RouteTimeSlotRepository routeTimeSlotRepository;
    private final RouteRepository routeRepository;
    private final TimeSlotRepository timeSlotRepository;

    @Transactional
    public RouteTimeSlot assignTimeSlotToRoute(Long routeId, Long timeSlotId) {
        Route route = routeRepository.findById(routeId)
            .orElseThrow(() -> new RuntimeException("Route not found"));

        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
            .orElseThrow(() -> new RuntimeException("Time slot not found"));

        if (routeTimeSlotRepository.existsByRouteAndTimeSlot(route, timeSlot)) {
            throw new RuntimeException("Time slot already assigned to this route");
        }

        RouteTimeSlot routeTimeSlot = new RouteTimeSlot();
        routeTimeSlot.setRoute(route);
        routeTimeSlot.setTimeSlot(timeSlot);
        routeTimeSlot.setIsActive(true);

        return routeTimeSlotRepository.save(routeTimeSlot);
    }

    public List<RouteTimeSlot> getTimeSlotsForRoute(Long routeId) {
        return routeTimeSlotRepository.findActiveByRouteId(routeId);
    }

    @Transactional
    public void removeTimeSlotFromRoute(Long routeTimeSlotId) {
        RouteTimeSlot routeTimeSlot = routeTimeSlotRepository.findById(routeTimeSlotId)
            .orElseThrow(() -> new RuntimeException("Route time slot assignment not found"));

        routeTimeSlot.setIsActive(false);
        routeTimeSlotRepository.save(routeTimeSlot);
    }
}