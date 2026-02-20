package backend.tdms.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
public class PackageEmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Send email when package is booked/sent
     */
    public boolean sendPackageSentEmail(String to, String senderName, String trackingNumber,
                                        String receiverName, String origin, String destination,
                                        String travelDate, String expectedArrival,
                                        String senderAddress, String receiverAddress) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject("Package Sent Successfully - " + trackingNumber);

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>
                    <div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd;'>
                        <div style='background-color: #1e40af; color: white; padding: 20px; text-align: center;'>
                            <h1 style='margin: 0;'>Package Sent Successfully</h1>
                        </div>
                        <div style='padding: 20px;'>
                            <p>Dear %s,</p>
                            <p>Your package has been successfully booked and will be sent on the scheduled trip.</p>
                            <div style='background-color: #f3f4f6; padding: 15px; margin: 20px 0; border-left: 4px solid #1e40af;'>
                                <h3 style='margin-top: 0; color: #1e40af;'>Package Details</h3>
                                <p><strong>Tracking Number:</strong> %s</p>
                                <p><strong>Receiver:</strong> %s</p>
                                <p><strong>Route:</strong> %s → %s</p>
                                <p><strong>Travel Date:</strong> %s</p>
                                <p><strong>Expected Arrival:</strong> %s</p>
                                <p><strong>Sender Address:</strong> %s</p>
                                <p><strong>Receiver Address:</strong> %s</p>
                            </div>
                            <div style='background-color: #fef3c7; padding: 15px; margin: 20px 0; border-left: 4px solid #f59e0b;'>
                                <p style='margin: 0;'><strong>📱 Track Your Package:</strong></p>
                                <p style='margin: 5px 0;'>Use tracking number <strong>%s</strong> to check status</p>
                                <p style='margin: 0;'>The receiver will be notified when the package arrives</p>
                            </div>
                            <p>Thank you for using our package delivery service!</p>
                            <p>Best regards,<br>TDMS Team</p>
                        </div>
                        <div style='background-color: #f9fafb; padding: 15px; text-align: center; font-size: 12px; color: #6b7280;'>
                            <p>This is an automated message. Please do not reply to this email.</p>
                            <p>For support, contact us at support@tdms.com</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(senderName, trackingNumber, receiverName, origin, destination,
                    travelDate, expectedArrival,
                    senderAddress != null ? senderAddress : "N/A",
                    receiverAddress != null ? receiverAddress : "N/A",
                    trackingNumber);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send email to receiver when package is on the way
     */
    public boolean sendPackageIncomingEmail(String to, String receiverName, String trackingNumber,
                                            String senderName, String senderPhone, String origin,
                                            String destination, String travelDate, String expectedArrival,
                                            String packageDescription, String senderAddress,
                                            String receiverAddress) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject("Package Coming Your Way - " + trackingNumber);

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>
                    <div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd;'>
                        <div style='background-color: #059669; color: white; padding: 20px; text-align: center;'>
                            <h1 style='margin: 0;'>📦 Package On The Way!</h1>
                        </div>
                        <div style='padding: 20px;'>
                            <p>Dear %s,</p>
                            <p>You have a package coming your way! Please prepare to collect it upon arrival.</p>
                            <div style='background-color: #f3f4f6; padding: 15px; margin: 20px 0; border-left: 4px solid #059669;'>
                                <h3 style='margin-top: 0; color: #059669;'>Package Information</h3>
                                <p><strong>Tracking Number:</strong> %s</p>
                                <p><strong>From:</strong> %s (%s)</p>
                                <p><strong>Sender Address:</strong> %s</p>
                                <p><strong>Route:</strong> %s → %s</p>
                                <p><strong>Travel Date:</strong> %s</p>
                                <p><strong>Expected Arrival:</strong> %s</p>
                                <p><strong>Description:</strong> %s</p>
                            </div>
                            <div style='background-color: #dbeafe; padding: 15px; margin: 20px 0; border-left: 4px solid #3b82f6;'>
                                <h4 style='margin-top: 0; color: #1e40af;'>Collection Instructions:</h4>
                                <ul style='margin: 5px 0; padding-left: 20px;'>
                                    <li>Bring your <strong>National ID</strong></li>
                                    <li>Your registered address: <strong>%s</strong></li>
                                    <li>Collection location: %s</li>
                                    <li>Operating hours: 8:00 AM - 6:00 PM</li>
                                    <li>You will receive another notification when package arrives</li>
                                </ul>
                            </div>
                            <p>We will notify you immediately when your package arrives at the destination.</p>
                            <p>Best regards,<br>TDMS Team</p>
                        </div>
                        <div style='background-color: #f9fafb; padding: 15px; text-align: center; font-size: 12px; color: #6b7280;'>
                            <p>Track your package: <strong>%s</strong></p>
                            <p>For support, contact us at support@tdms.com</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(receiverName, trackingNumber, senderName, senderPhone,
                    senderAddress != null ? senderAddress : "N/A",
                    origin, destination, travelDate, expectedArrival, packageDescription,
                    receiverAddress != null ? receiverAddress : "N/A",
                    destination, trackingNumber);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send email when package arrives at destination
     */
    public boolean sendPackageArrivedEmail(String to, String receiverName, String trackingNumber,
                                           String arrivalTime, String collectionLocation,
                                           String receiverAddress) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject("Package Arrived - Ready for Collection - " + trackingNumber);

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>
                    <div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd;'>
                        <div style='background-color: #10b981; color: white; padding: 20px; text-align: center;'>
                            <h1 style='margin: 0;'>✅ Package Arrived!</h1>
                        </div>
                        <div style='padding: 20px;'>
                            <p>Dear %s,</p>
                            <p style='font-size: 18px; color: #10b981; font-weight: bold;'>
                                Great news! Your package has arrived and is ready for collection.
                            </p>
                            <div style='background-color: #f3f4f6; padding: 15px; margin: 20px 0; border-left: 4px solid #10b981;'>
                                <p><strong>Tracking Number:</strong> %s</p>
                                <p><strong>Arrival Time:</strong> %s</p>
                                <p><strong>Collection Location:</strong> %s</p>
                                <p><strong>Your Registered Address:</strong> %s</p>
                            </div>
                            <div style='background-color: #fef3c7; padding: 15px; margin: 20px 0; border-left: 4px solid #f59e0b;'>
                                <h4 style='margin-top: 0; color: #b45309;'>⚠️ Important - What to Bring:</h4>
                                <ul style='margin: 5px 0; padding-left: 20px;'>
                                    <li><strong>Your National ID</strong> (Required for verification)</li>
                                    <li>Tracking Number: <strong>%s</strong></li>
                                </ul>
                            </div>
                            <div style='background-color: #dbeafe; padding: 15px; margin: 20px 0;'>
                                <h4 style='margin-top: 0; color: #1e40af;'>Operating Hours:</h4>
                                <p style='margin: 5px 0;'><strong>Monday - Saturday:</strong> 8:00 AM - 6:00 PM</p>
                                <p style='margin: 5px 0;'><strong>Sunday:</strong> Closed</p>
                            </div>
                            <p>Please collect your package as soon as possible. Storage is free for the first 48 hours.</p>
                            <p>Best regards,<br>TDMS Team</p>
                        </div>
                        <div style='background-color: #f9fafb; padding: 15px; text-align: center; font-size: 12px; color: #6b7280;'>
                            <p>For support or inquiries, contact us at support@tdms.com</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(receiverName, trackingNumber, arrivalTime, collectionLocation,
                    receiverAddress != null ? receiverAddress : "N/A",
                    trackingNumber);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send email to sender when package is collected
     */
    public boolean sendPackageDeliveredEmail(String to, String senderName, String trackingNumber,
                                             String receiverName, String collectionTime,
                                             String senderAddress, String receiverAddress) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject("Package Delivered Successfully - " + trackingNumber);

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>
                    <div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd;'>
                        <div style='background-color: #10b981; color: white; padding: 20px; text-align: center;'>
                            <h1 style='margin: 0;'>✅ Package Delivered!</h1>
                        </div>
                        <div style='padding: 20px;'>
                            <p>Dear %s,</p>
                            <p style='font-size: 18px; color: #10b981; font-weight: bold;'>
                                Your package has been successfully delivered!
                            </p>
                            <div style='background-color: #f3f4f6; padding: 15px; margin: 20px 0; border-left: 4px solid #10b981;'>
                                <h3 style='margin-top: 0; color: #10b981;'>Delivery Confirmation</h3>
                                <p><strong>Tracking Number:</strong> %s</p>
                                <p><strong>Collected By:</strong> %s</p>
                                <p><strong>Collection Time:</strong> %s</p>
                                <p><strong>Sender Address:</strong> %s</p>
                                <p><strong>Receiver Address:</strong> %s</p>
                                <p><strong>Status:</strong> <span style='color: #10b981;'>✓ DELIVERED</span></p>
                            </div>
                            <div style='background-color: #dbeafe; padding: 15px; margin: 20px 0;'>
                                <p style='margin: 0;'>
                                    The receiver has collected the package and verified their identity with their National ID.
                                </p>
                            </div>
                            <p>Thank you for using our package delivery service. We appreciate your trust in us!</p>
                            <p>Best regards,<br>TDMS Team</p>
                        </div>
                        <div style='background-color: #f9fafb; padding: 15px; text-align: center; font-size: 12px; color: #6b7280;'>
                            <p>Thank you for choosing TDMS Package Delivery Service</p>
                            <p>For support, contact us at support@tdms.com</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(senderName, trackingNumber, receiverName, collectionTime,
                    senderAddress != null ? senderAddress : "N/A",
                    receiverAddress != null ? receiverAddress : "N/A");

            helper.setText(htmlContent, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}