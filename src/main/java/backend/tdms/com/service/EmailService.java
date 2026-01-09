package backend.tdms.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public boolean sendWelcomeEmail(String to, String firstName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Welcome to TDMS - Transport Data Management System!");
            message.setText("Dear " + firstName + ",\n\n" +
                    "Welcome to our Transport Data Management System! Your account has been successfully created.\n\n" +
                    "You can now log in to access the system using your email and password.\n\n" +
                    "Login URL: http://localhost:3000/login\n\n" +
                    "If you have any questions or need assistance, please don't hesitate to contact us.\n\n" +
                    "Best regards,\n" +
                    "The TDMS Team");
            mailSender.send(message);
            return true;
        } catch (MailSendException e) {
            return false;
        }
    }

    public boolean sendAdminCredentialsEmail(String to, String password) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Your TDMS Admin Account Credentials");
            message.setText("Dear Admin,\n\n" +
                    "Your administrator account has been created for the Transport Data Management System.\n\n" +
                    "Login Credentials:\n" +
                    "Email: " + to + "\n" +
                    "Password: " + password + "\n\n" +
                    "Login URL: http://localhost:3000/login\n\n" +
                    "IMPORTANT: For security reasons, you will be required to change your password upon first login.\n\n" +
                    "Please keep these credentials secure and do not share them with anyone.\n\n" +
                    "Best regards,\n" +
                    "The TDMS Team");
            mailSender.send(message);
            return true;
        } catch (MailSendException e) {
            return false;
        }
    }

    public boolean sendUserCredentialsEmail(String to, String password, String role) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Your TDMS Account Credentials");
            
            // Format role name nicely
            String roleName = role.replace("ROLE_", "").replace("_", " ");
            roleName = roleName.substring(0, 1).toUpperCase() + roleName.substring(1).toLowerCase();
            
            message.setText("Dear User,\n\n" +
                    "Your account has been created in the Transport Data Management System.\n\n" +
                    "Account Details:\n" +
                    "Email: " + to + "\n" +
                    "Password: " + password + "\n" +
                    "Role: " + roleName + "\n\n" +
                    "Login URL: http://localhost:3000/login\n\n" +
                    "IMPORTANT: You will be required to:\n" +
                    "1. Change your password upon first login\n" +
                    "2. Complete your profile information\n\n" +
                    "Please keep these credentials secure and do not share them with anyone.\n\n" +
                    "Best regards,\n" +
                    "The TDMS Team");
            mailSender.send(message);
            return true;
        } catch (MailSendException e) {
            return false;
        }
    }

    public boolean sendPasswordResetEmail(String to, String resetToken, String firstName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("TDMS - Password Reset Request");
            
            String greeting = (firstName != null && !firstName.isEmpty()) 
                ? "Dear " + firstName + "," 
                : "Dear User,";
            
            message.setText(greeting + "\n\n" +
                    "We received a request to reset your password for your Transport Data Management System account.\n\n" +
                    "Please click the following link to reset your password:\n" +
                    "http://localhost:3000/reset-password?token=" + resetToken + "\n\n" +
                    "This link will expire in 24 hours for security reasons.\n\n" +
                    "If you did not request a password reset, please ignore this email and your password will remain unchanged.\n\n" +
                    "For security reasons, never share this link with anyone.\n\n" +
                    "Best regards,\n" +
                    "The TDMS Team");
            mailSender.send(message);
            return true;
        } catch (MailSendException e) {
            return false;
        }
    }

    public boolean sendEmailChangeNotification(String oldEmail, String newEmail, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            // Send to old email
            helper.setTo(oldEmail);
            helper.setSubject("Email Address Changed - TDMS");
            
            String htmlContent = "<!DOCTYPE html>" +
                "<html><body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2 style='color: #1e40af;'>Email Address Changed</h2>" +
                "<p>Hello " + firstName + ",</p>" +
                "<p>Your TDMS account email has been changed from <strong>" + oldEmail + "</strong> to <strong>" + newEmail + "</strong>.</p>" +
                "<p>If you did not make this change, please contact support immediately.</p>" +
                "<p>Best regards,<br>TDMS Team</p>" +
                "</div></body></html>";
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            
            // Send to new email
            MimeMessage newMessage = mailSender.createMimeMessage();
            MimeMessageHelper newHelper = new MimeMessageHelper(newMessage, true);
            newHelper.setTo(newEmail);
            newHelper.setSubject("Email Address Confirmed - TDMS");
            
            String newHtmlContent = "<!DOCTYPE html>" +
                "<html><body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2 style='color: #1e40af;'>Email Address Confirmed</h2>" +
                "<p>Hello " + firstName + ",</p>" +
                "<p>This email confirms that your TDMS account email has been successfully changed to this address.</p>" +
                "<p>You can now use this email to log in to your account.</p>" +
                "<p>Best regards,<br>TDMS Team</p>" +
                "</div></body></html>";
            
            newHelper.setText(newHtmlContent, true);
            mailSender.send(newMessage);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendAccountDeletionEmail(String email, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(email);
            helper.setSubject("Account Deleted - TDMS");
            
            String htmlContent = "<!DOCTYPE html>" +
                "<html><body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2 style='color: #1e40af;'>Account Deletion Confirmed</h2>" +
                "<p>Hello " + firstName + ",</p>" +
                "<p>Your TDMS account has been successfully deleted.</p>" +
                "<p>We're sorry to see you go. If you change your mind, you can create a new account anytime.</p>" +
                "<p>Thank you for using TDMS.</p>" +
                "<p>Best regards,<br>TDMS Team</p>" +
                "</div></body></html>";
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}