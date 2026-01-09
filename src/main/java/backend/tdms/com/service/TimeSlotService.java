package backend.tdms.com.service;

import backend.tdms.com.dto.TimeSlotDTO;
import backend.tdms.com.model.TimeSlot;
import backend.tdms.com.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;

    @Transactional
    public TimeSlot createTimeSlot(TimeSlotDTO dto) {
        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setDepartureTime(dto.getDepartureTime());
        timeSlot.setIsActive(true);

        return timeSlotRepository.save(timeSlot);
    }

    public List<TimeSlot> getAllActiveTimeSlots() {
        return timeSlotRepository.findByIsActiveTrueOrderByDepartureTimeAsc();
    }

    public TimeSlot getTimeSlotById(Long id) {
        return timeSlotRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Time slot not found"));
    }

    @Transactional
    public TimeSlot updateTimeSlot(Long id, TimeSlotDTO dto) {
        TimeSlot timeSlot = getTimeSlotById(id);
        timeSlot.setDepartureTime(dto.getDepartureTime());
        timeSlot.setIsActive(dto.getIsActive());

        return timeSlotRepository.save(timeSlot);
    }

    @Transactional
    public void deleteTimeSlot(Long id) {
        TimeSlot timeSlot = getTimeSlotById(id);
        timeSlot.setIsActive(false);
        timeSlotRepository.save(timeSlot);
    }
}