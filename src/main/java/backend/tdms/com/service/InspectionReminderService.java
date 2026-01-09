package backend.tdms.com.service;

import backend.tdms.com.model.VehicleInspection;
import backend.tdms.com.repository.VehicleInspectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionReminderService {

    private final VehicleInspectionRepository inspectionRepository;
    
    @Autowired
    private JavaMailSender mailSender;

    // Email to send reminders to (could be from properties or database)
    private static final String ADMIN_EMAIL = "admin@tdms.com"; // Configure this

    /**
     * Runs every day at 8:00 AM
     * Checks for inspections due soon and sends reminders
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDailyInspectionReminders() {
        log.info("Running daily inspection reminder check...");

        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysLater = today.plusDays(30);

        // Get inspections due in next 30 days
        List<VehicleInspection> dueSoon = inspectionRepository.findDueSoon(today, thirtyDaysLater);

        // Get overdue inspections
        List<VehicleInspection> overdue = inspectionRepository.findOverdue(today);

        // Send reminders
        if (!dueSoon.isEmpty()) {
            sendDueSoonReminder(dueSoon);
        }

        if (!overdue.isEmpty()) {
            sendOverdueAlert(overdue);
        }

        log.info("Inspection reminder check completed. Due soon: {}, Overdue: {}", 
            dueSoon.size(), overdue.size());
    }

    /**
     * Send reminder for inspections due soon
     */
    private void sendDueSoonReminder(List<VehicleInspection> inspections) {
        try {
            StringBuilder message = new StringBuilder();
            message.append("The following vehicles are due for government inspection within 30 days:\n\n");

            for (VehicleInspection inspection : inspections) {
                long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDate.now(), 
                    inspection.getNextInspectionDue()
                );

                message.append(String.format("• Vehicle: %s\n", 
                    inspection.getVehicle().getPlateNo()));
                message.append(String.format("  Last Inspection: %s\n", 
                    inspection.getInspectionDate()));
                message.append(String.format("  Due Date: %s (%d days)\n\n", 
                    inspection.getNextInspectionDue(), daysUntil));
            }

            message.append("\nPlease schedule inspections accordingly.\n");
            message.append("This is an automated reminder from TDMS.");

            sendEmail(
                ADMIN_EMAIL,
                "Vehicle Inspections Due Soon - Action Required",
                message.toString()
            );

            log.info("Sent 'due soon' reminder for {} vehicles", inspections.size());
        } catch (Exception e) {
            log.error("Failed to send due soon reminder: {}", e.getMessage());
        }
    }

    /**
     * Send alert for overdue inspections
     */
    private void sendOverdueAlert(List<VehicleInspection> inspections) {
        try {
            StringBuilder message = new StringBuilder();
            message.append("⚠️ URGENT: The following vehicles have OVERDUE government inspections:\n\n");

            for (VehicleInspection inspection : inspections) {
                long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                    inspection.getNextInspectionDue(),
                    LocalDate.now()
                );

                message.append(String.format("• Vehicle: %s\n", 
                    inspection.getVehicle().getPlateNo()));
                message.append(String.format("  Last Inspection: %s\n", 
                    inspection.getInspectionDate()));
                message.append(String.format("  Was Due: %s (%d days overdue)\n", 
                    inspection.getNextInspectionDue(), daysOverdue));
                message.append(String.format("  Status: %s\n\n", 
                    inspection.getInspectionStatus()));
            }

            message.append("\n⚠️ IMMEDIATE ACTION REQUIRED\n");
            message.append("These vehicles should not be in operation until inspected.\n");
            message.append("This is an automated alert from TDMS.");

            sendEmail(
                ADMIN_EMAIL,
                "🚨 URGENT: Overdue Vehicle Inspections",
                message.toString()
            );

            log.info("Sent overdue alert for {} vehicles", inspections.size());
        } catch (Exception e) {
            log.error("Failed to send overdue alert: {}", e.getMessage());
        }
    }

    /**
     * Send email using mail sender
     */
    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom("noreply@tdms.com");

            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage());
        }
    }

    /**
     * Manual trigger for testing (can be called from controller)
     */
    public void sendTestReminder() {
        log.info("Sending test inspection reminders...");
        sendDailyInspectionReminders();
    }
}