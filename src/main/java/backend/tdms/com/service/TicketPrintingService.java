package backend.tdms.com.service;

import backend.tdms.com.model.Booking;
import backend.tdms.com.model.DailyTrip;
import backend.tdms.com.model.Driver;
import backend.tdms.com.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class TicketPrintingService {

    private final DriverRepository driverRepository;

    /**
     * Generate ticket as plain text for printing
     */
    public String generateTicketText(Booking booking) {
        DailyTrip trip = booking.getDailyTrip();
        
        // Get driver info if available
        String driverInfo = "Not assigned";
        try {
            Driver driver = driverRepository.findByAssignedVehicle(trip.getVehicle())
                .orElse(null);
            if (driver != null) {
                driverInfo = driver.getNames() + " (" + driver.getPhoneNumber() + ")";
            }
        } catch (Exception e) {
            // Driver info not critical, continue
        }

        StringBuilder ticket = new StringBuilder();
        ticket.append("=====================================\n");
        ticket.append("      TRANSPORT BOOKING TICKET\n");
        ticket.append("=====================================\n\n");
        
        ticket.append("Ticket Number: ").append(booking.getTicketNumber()).append("\n");
        ticket.append("Booking Date: ").append(formatDateTime(booking.getBookingDate())).append("\n\n");
        
        ticket.append("-------------------------------------\n");
        ticket.append("PASSENGER INFORMATION\n");
        ticket.append("-------------------------------------\n");
        ticket.append("Name: ").append(booking.getCustomer().getNames()).append("\n");
        ticket.append("Phone: ").append(booking.getCustomer().getPhoneNumber()).append("\n");
        
        ticket.append("\n");
        
        ticket.append("-------------------------------------\n");
        ticket.append("TRIP DETAILS\n");
        ticket.append("-------------------------------------\n");
        ticket.append("Route: ").append(trip.getRoute().getOrigin())
               .append(" → ").append(trip.getRoute().getDestination()).append("\n");
        ticket.append("Travel Date: ").append(formatDate(trip.getTripDate())).append("\n");
        ticket.append("Departure Time: ").append(formatTime(trip.getTimeSlot().getDepartureTime())).append("\n");
        
        if (trip.getRoute().getDurationMinutes() != null) {
            int hours = trip.getRoute().getDurationMinutes() / 60;
            int minutes = trip.getRoute().getDurationMinutes() % 60;
            ticket.append("Duration: ").append(hours).append("h ").append(minutes).append("min\n");
        }
        ticket.append("\n");
        
        ticket.append("-------------------------------------\n");
        ticket.append("VEHICLE & DRIVER\n");
        ticket.append("-------------------------------------\n");
        ticket.append("Vehicle: ").append(trip.getVehicle().getPlateNo()).append("\n");
        ticket.append("Type: ").append(trip.getVehicle().getVehicleType()).append("\n");
        ticket.append("Seat Number: ").append(booking.getSeatNumber()).append("\n");
        ticket.append("Driver: ").append(driverInfo).append("\n\n");
        
        ticket.append("-------------------------------------\n");
        ticket.append("PAYMENT INFORMATION\n");
        ticket.append("-------------------------------------\n");
        ticket.append("Price: RWF ").append(booking.getPrice()).append("\n");
        ticket.append("Payment Method: ").append(booking.getPaymentMethod()).append("\n");
        ticket.append("Payment Status: ").append(booking.getPaymentStatus()).append("\n");
        
        ticket.append("=====================================\n");
        ticket.append("IMPORTANT INFORMATION\n");
        ticket.append("=====================================\n");
        ticket.append("• Please arrive 15 minutes early\n");
        ticket.append("• Present this ticket before boarding\n");
        ticket.append("• Keep this ticket for your records\n");
        ticket.append("• No refunds on departure day\n\n");
        
        ticket.append("Thank you for choosing our service!\n");
        ticket.append("=====================================\n");

        return ticket.toString();
    }

    /**
     * Generate ticket as HTML for web display/printing
     */
    public String generateTicketHTML(Booking booking) {
        DailyTrip trip = booking.getDailyTrip();
        
        String driverInfo = "Not assigned";
        try {
            Driver driver = driverRepository.findByAssignedVehicle(trip.getVehicle())
                .orElse(null);
            if (driver != null) {
                driverInfo = driver.getNames() + " (" + driver.getPhoneNumber() + ")";
            }
        } catch (Exception e) {
            // Driver info not critical
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Ticket - %s</title>
                <style>
                    body {
                        font-family: 'Courier New', monospace;
                        max-width: 400px;
                        margin: 20px auto;
                        padding: 20px;
                        border: 2px solid #000;
                    }
                    .header {
                        text-align: center;
                        border-bottom: 2px solid #000;
                        padding-bottom: 10px;
                        margin-bottom: 20px;
                    }
                    .section {
                        margin: 15px 0;
                        padding: 10px 0;
                        border-bottom: 1px dashed #000;
                    }
                    .section-title {
                        font-weight: bold;
                        font-size: 14px;
                        margin-bottom: 5px;
                    }
                    .row {
                        display: flex;
                        justify-content: space-between;
                        margin: 5px 0;
                    }
                    .label {
                        font-weight: bold;
                    }
                    .footer {
                        text-align: center;
                        font-size: 12px;
                        margin-top: 20px;
                        padding-top: 10px;
                        border-top: 2px solid #000;
                    }
                    @media print {
                        body {
                            border: none;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h2>TRANSPORT BOOKING TICKET</h2>
                    <p>%s</p>
                </div>
                
                <div class="section">
                    <div class="row">
                        <span class="label">Ticket Number:</span>
                        <span>%s</span>
                    </div>
                    <div class="row">
                        <span class="label">Booking Date:</span>
                        <span>%s</span>
                    </div>
                </div>
                
                <div class="section">
                    <div class="section-title">PASSENGER</div>
                    <div class="row">
                        <span class="label">Name:</span>
                        <span>%s</span>
                    </div>
                    <div class="row">
                        <span class="label">Phone:</span>
                        <span>%s</span>
                    </div>
                </div>
                
                <div class="section">
                    <div class="section-title">TRIP DETAILS</div>
                    <div class="row">
                        <span class="label">Route:</span>
                        <span>%s → %s</span>
                    </div>
                    <div class="row">
                        <span class="label">Travel Date:</span>
                        <span>%s</span>
                    </div>
                    <div class="row">
                        <span class="label">Departure:</span>
                        <span>%s</span>
                    </div>
                </div>
                
                <div class="section">
                    <div class="section-title">VEHICLE & DRIVER</div>
                    <div class="row">
                        <span class="label">Vehicle:</span>
                        <span>%s</span>
                    </div>
                    <div class="row">
                        <span class="label">Type:</span>
                        <span>%s</span>
                    </div>
                    <div class="row">
                        <span class="label">Seat:</span>
                        <span>%s</span>
                    </div>
                    <div class="row">
                        <span class="label">Driver:</span>
                        <span>%s</span>
                    </div>
                </div>
                
                <div class="section">
                    <div class="section-title">PAYMENT</div>
                    <div class="row">
                        <span class="label">Amount:</span>
                        <span>RWF %s</span>
                    </div>
                    <div class="row">
                        <span class="label">Method:</span>
                        <span>%s</span>
                    </div>
                    <div class="row">
                        <span class="label">Status:</span>
                        <span>%s</span>
                    </div>
                </div>
                
                <div class="footer">
                    <p><strong>Important:</strong></p>
                    <p>Please arrive 15 minutes before departure</p>
                    <p>Present this ticket before boarding</p>
                    <p>Thank you for choosing our service!</p>
                </div>
                
                <script>
                    window.onload = function() {
                        window.print();
                    }
                </script>
            </body>
            </html>
            """.formatted(
            booking.getTicketNumber(),
            booking.getTicketNumber(),
            booking.getTicketNumber(),
            formatDateTime(booking.getBookingDate()),
            booking.getCustomer().getNames(),
            booking.getCustomer().getPhoneNumber(),
            trip.getRoute().getOrigin(),
            trip.getRoute().getDestination(),
            formatDate(trip.getTripDate()),
            formatTime(trip.getTimeSlot().getDepartureTime()),
            trip.getVehicle().getPlateNo(),
            trip.getVehicle().getVehicleType(),
            booking.getSeatNumber(),
            driverInfo,
            booking.getPrice(),
            booking.getPaymentMethod(),
            booking.getPaymentStatus()
        );
    }

    /**
     * Generate receipt after booking
     */
    public String generateReceipt(Booking booking) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("\n");
        receipt.append("************************************\n");
        receipt.append("        PAYMENT RECEIPT\n");
        receipt.append("************************************\n\n");
        receipt.append("Ticket: ").append(booking.getTicketNumber()).append("\n");
        receipt.append("Date: ").append(formatDateTime(booking.getBookingDate())).append("\n\n");
        receipt.append("Customer: ").append(booking.getCustomer().getNames()).append("\n");
        receipt.append("Route: ").append(booking.getDailyTrip().getRoute().getOrigin())
               .append(" → ").append(booking.getDailyTrip().getRoute().getDestination()).append("\n");
        receipt.append("Amount Paid: RWF ").append(booking.getPrice()).append("\n");
        receipt.append("Payment Method: ").append(booking.getPaymentMethod()).append("\n");
        receipt.append("\n************************************\n");
        receipt.append("     Thank you for your payment!\n");
        receipt.append("************************************\n\n");
        
        return receipt.toString();
    }

    private String formatDateTime(java.time.LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateTime.format(formatter);
    }

    private String formatDate(java.time.LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return date.format(formatter);
    }

    private String formatTime(java.time.LocalTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return time.format(formatter);
    }
    // Add this new method to TicketPrintingService.java
    public String generateTicketHTMLForDownload(Booking booking) {
        DailyTrip trip = booking.getDailyTrip();
        
        String driverInfo = "Not assigned";
        try {
            Driver driver = driverRepository.findByAssignedVehicle(trip.getVehicle())
                .orElse(null);
            if (driver != null) {
                driverInfo = driver.getNames() + " (" + driver.getPhoneNumber() + ")";
            }
        } catch (Exception e) {
            // Driver info not critical
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Ticket - %s</title>
                <style>
                    @page {
                        size: A5;
                        margin: 0;
                    }
                    body {
                        font-family: Arial, sans-serif;
                        margin: 0;
                        padding: 20px;
                        background: white;
                    }
                    .ticket-container {
                        max-width: 400px;
                        margin: 0 auto;
                        border: 2px solid #000;
                        padding: 20px;
                    }
                    .header {
                        text-align: center;
                        border-bottom: 2px solid #000;
                        padding-bottom: 15px;
                        margin-bottom: 20px;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 24px;
                        color: #1e40af;
                    }
                    .ticket-number {
                        font-size: 18px;
                        font-weight: bold;
                        color: #1e40af;
                        margin-top: 10px;
                    }
                    .section {
                        margin: 20px 0;
                        padding: 15px 0;
                        border-bottom: 1px dashed #666;
                    }
                    .section:last-child {
                        border-bottom: none;
                    }
                    .section-title {
                        font-weight: bold;
                        font-size: 14px;
                        color: #374151;
                        margin-bottom: 10px;
                        text-transform: uppercase;
                    }
                    .info-row {
                        display: flex;
                        justify-content: space-between;
                        margin: 8px 0;
                        font-size: 14px;
                    }
                    .label {
                        font-weight: 600;
                        color: #6b7280;
                    }
                    .value {
                        font-weight: 500;
                        color: #111827;
                    }
                    .route {
                        font-size: 18px;
                        font-weight: bold;
                        color: #1e40af;
                        text-align: center;
                        margin: 10px 0;
                    }
                    .footer {
                        text-align: center;
                        font-size: 12px;
                        margin-top: 20px;
                        padding-top: 15px;
                        border-top: 2px solid #000;
                        color: #6b7280;
                    }
                    .footer ul {
                        list-style: none;
                        padding: 0;
                        margin: 10px 0;
                    }
                    .footer li {
                        margin: 5px 0;
                    }
                    @media print {
                        body {
                            padding: 0;
                        }
                        .ticket-container {
                            border: none;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="ticket-container">
                    <div class="header">
                        <h1>TRANSPORT TICKET</h1>
                        <div class="ticket-number">%s</div>
                        <div style="font-size: 12px; color: #6b7280; margin-top: 5px;">%s</div>
                    </div>
                    
                    <div class="section">
                        <div class="section-title">Passenger Information</div>
                        <div class="info-row">
                            <span class="label">Name:</span>
                            <span class="value">%s</span>
                        </div>
                        <div class="info-row">
                            <span class="label">Phone:</span>
                            <span class="value">%s</span>
                        </div>
                    </div>
                    
                    <div class="section">
                        <div class="section-title">Trip Details</div>
                        <div class="route">%s → %s</div>
                        <div class="info-row">
                            <span class="label">Travel Date:</span>
                            <span class="value">%s</span>
                        </div>
                        <div class="info-row">
                            <span class="label">Departure Time:</span>
                            <span class="value">%s</span>
                        </div>
                    </div>
                    
                    <div class="section">
                        <div class="section-title">Vehicle & Seat</div>
                        <div class="info-row">
                            <span class="label">Vehicle:</span>
                            <span class="value">%s (%s)</span>
                        </div>
                        <div class="info-row">
                            <span class="label">Seat Number:</span>
                            <span class="value" style="font-size: 20px; color: #1e40af;">%s</span>
                        </div>
                        <div class="info-row">
                            <span class="label">Driver:</span>
                            <span class="value">%s</span>
                        </div>
                    </div>
                    
                    <div class="section">
                        <div class="section-title">Payment Information</div>
                        <div class="info-row">
                            <span class="label">Amount Paid:</span>
                            <span class="value" style="font-size: 18px; color: #1e40af;">RWF %s</span>
                        </div>
                        <div class="info-row">
                            <span class="label">Payment Method:</span>
                            <span class="value">%s</span>
                        </div>
                    </div>
                    
                    <div class="footer">
                        <strong>Important Information</strong>
                        <ul>
                            <li>✓ Arrive 15 minutes before departure</li>
                            <li>✓ Present this ticket when boarding</li>
                            <li>✓ Keep for your records</li>
                            <li>✓ No refunds on departure day</li>
                        </ul>
                        <p style="margin-top: 15px;">Thank you for choosing our service!</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
            booking.getTicketNumber(),
            booking.getTicketNumber(),
            formatDateTime(booking.getBookingDate()),
            booking.getCustomer().getNames(),
            booking.getCustomer().getPhoneNumber(),
            trip.getRoute().getOrigin(),
            trip.getRoute().getDestination(),
            formatDate(trip.getTripDate()),
            formatTime(trip.getTimeSlot().getDepartureTime()),
            trip.getVehicle().getPlateNo(),
            trip.getVehicle().getVehicleType(),
            booking.getSeatNumber(),
            driverInfo,
            booking.getPrice(),
            booking.getPaymentMethod()
        );
    }
}