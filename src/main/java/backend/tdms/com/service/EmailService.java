package backend.tdms.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j  // ✅ This adds the logger
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // ✅ Add fromEmail field with default value
    @Value("${spring.mail.username:noreply@tdms.com}")
    private String fromEmail;

    // ========================================
    // PROFESSIONAL HTML EMAIL TEMPLATES
    // ========================================

    public boolean sendWelcomeEmail(String to, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);  // ✅ Now fromEmail is defined
            helper.setTo(to);
            helper.setSubject("Welcome to TDMS - Transport Data Management System! 🎉");
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style='margin: 0; padding: 0; background-color: #f3f4f6; font-family: Arial, sans-serif;'>
                    <div style='max-width: 600px; margin: 0 auto; background-color: #ffffff;'>
                        <!-- Header -->
                        <div style='background: linear-gradient(135deg, #1e40af 0%%, #3b82f6 100%%); padding: 40px 20px; text-align: center;'>
                            <h1 style='color: #ffffff; margin: 0; font-size: 32px; font-weight: bold;'>
                                <span style='color: #60a5fa;'>TD</span>MS
                            </h1>
                            <p style='color: #dbeafe; margin: 10px 0 0 0; font-size: 16px;'>Transport Data Management System</p>
                        </div>
                        
                        <!-- Content -->
                        <div style='padding: 40px 30px;'>
                            <h2 style='color: #1e40af; margin: 0 0 20px 0; font-size: 24px;'>Welcome, %s! 🎉</h2>
                            
                            <p style='color: #4b5563; line-height: 1.6; margin: 0 0 20px 0; font-size: 16px;'>
                                Thank you for joining TDMS! Your account has been successfully created and you're ready to get started.
                            </p>
                            
                            <!-- Info Box -->
                            <div style='background-color: #eff6ff; border-left: 4px solid #3b82f6; padding: 20px; margin: 30px 0; border-radius: 4px;'>
                                <h3 style='color: #1e40af; margin: 0 0 15px 0; font-size: 18px;'>✨ What's Next?</h3>
                                <ul style='color: #1e40af; margin: 0; padding-left: 20px; line-height: 1.8;'>
                                    <li>Log in to your account using your email and password</li>
                                    <li>Complete your profile information</li>
                                    <li>Explore the system features</li>
                                </ul>
                            </div>
                            
                            <!-- CTA Button -->
                            <div style='text-align: center; margin: 30px 0;'>
                                <a href='http://localhost:3000/login' style='display: inline-block; background: linear-gradient(135deg, #1e40af 0%%, #3b82f6 100%%); color: #ffffff; text-decoration: none; padding: 14px 40px; border-radius: 8px; font-weight: bold; font-size: 16px;'>
                                    Login to TDMS
                                </a>
                            </div>
                            
                            <p style='color: #6b7280; font-size: 14px; margin: 30px 0 0 0; line-height: 1.6;'>
                                If you have any questions or need assistance, please don't hesitate to contact our support team.
                            </p>
                        </div>
                        
                        <!-- Footer -->
                        <div style='background-color: #f9fafb; padding: 30px; text-align: center; border-top: 1px solid #e5e7eb;'>
                            <p style='color: #9ca3af; font-size: 14px; margin: 0;'>
                                © 2026 TDMS. All rights reserved.
                            </p>
                            <p style='color: #9ca3af; font-size: 12px; margin: 10px 0 0 0;'>
                                Transport Data Management System
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """, firstName);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", to, e.getMessage());  // ✅ Now log is available
            return false;
        }
    }

    public boolean sendAdminCredentialsEmail(String to, String password) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("🔐 Your TDMS Admin Account Credentials");
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style='margin: 0; padding: 0; background-color: #f3f4f6; font-family: Arial, sans-serif;'>
                    <div style='max-width: 600px; margin: 0 auto; background-color: #ffffff;'>
                        <!-- Header -->
                        <div style='background: linear-gradient(135deg, #7c3aed 0%%, #a855f7 100%%); padding: 40px 20px; text-align: center;'>
                            <div style='background-color: rgba(255,255,255,0.2); width: 80px; height: 80px; border-radius: 50%%; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center;'>
                                <span style='font-size: 40px;'>🔐</span>
                            </div>
                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>Administrator Account</h1>
                            <p style='color: #e9d5ff; margin: 10px 0 0 0;'>TDMS Admin Access</p>
                        </div>
                        
                        <!-- Content -->
                        <div style='padding: 40px 30px;'>
                            <h2 style='color: #7c3aed; margin: 0 0 20px 0; font-size: 22px;'>Welcome, Administrator!</h2>
                            
                            <p style='color: #4b5563; line-height: 1.6; margin: 0 0 20px 0;'>
                                Your administrator account has been created for the Transport Data Management System.
                            </p>
                            
                            <!-- Credentials Box -->
                            <div style='background: linear-gradient(135deg, #faf5ff 0%%, #f3e8ff 100%%); border: 2px solid #a855f7; padding: 25px; margin: 30px 0; border-radius: 12px;'>
                                <h3 style='color: #7c3aed; margin: 0 0 20px 0; font-size: 18px; text-align: center;'>🔑 Login Credentials</h3>
                                <div style='background-color: #ffffff; padding: 20px; border-radius: 8px; margin-bottom: 15px;'>
                                    <p style='color: #6b7280; margin: 0 0 5px 0; font-size: 14px;'>Email</p>
                                    <p style='color: #1f2937; margin: 0; font-size: 16px; font-weight: bold; word-break: break-all;'>%s</p>
                                </div>
                                <div style='background-color: #ffffff; padding: 20px; border-radius: 8px;'>
                                    <p style='color: #6b7280; margin: 0 0 5px 0; font-size: 14px;'>Temporary Password</p>
                                    <p style='color: #1f2937; margin: 0; font-size: 16px; font-weight: bold; font-family: monospace; letter-spacing: 1px;'>%s</p>
                                </div>
                            </div>
                            
                            <!-- Warning Box -->
                            <div style='background-color: #fef3c7; border-left: 4px solid #f59e0b; padding: 20px; margin: 30px 0; border-radius: 4px;'>
                                <h3 style='color: #92400e; margin: 0 0 10px 0; font-size: 16px;'>⚠️ Important Security Notice</h3>
                                <ul style='color: #92400e; margin: 0; padding-left: 20px; line-height: 1.8; font-size: 14px;'>
                                    <li>You will be required to change your password upon first login</li>
                                    <li>Never share your credentials with anyone</li>
                                    <li>Use a strong, unique password</li>
                                    <li>Enable two-factor authentication if available</li>
                                </ul>
                            </div>
                            
                            <!-- CTA Button -->
                            <div style='text-align: center; margin: 30px 0;'>
                                <a href='http://localhost:3000/login' style='display: inline-block; background: linear-gradient(135deg, #7c3aed 0%%, #a855f7 100%%); color: #ffffff; text-decoration: none; padding: 14px 40px; border-radius: 8px; font-weight: bold; font-size: 16px;'>
                                    Access Admin Panel
                                </a>
                            </div>
                        </div>
                        
                        <!-- Footer -->
                        <div style='background-color: #f9fafb; padding: 30px; text-align: center; border-top: 1px solid #e5e7eb;'>
                            <p style='color: #9ca3af; font-size: 14px; margin: 0;'>
                                © 2026 TDMS. All rights reserved.
                            </p>
                            <p style='color: #9ca3af; font-size: 12px; margin: 10px 0 0 0;'>
                                This is a secure system notification. Please do not reply to this email.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """, to, password);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send admin credentials email to {}: {}", to, e.getMessage());
            return false;
        }
    }

    public boolean sendUserCredentialsEmail(String to, String password, String role) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("🎫 Your TDMS Account Credentials");
            
            // Format role name nicely
            String roleName = role.replace("ROLE_", "").replace("_", " ");
            roleName = roleName.substring(0, 1).toUpperCase() + roleName.substring(1).toLowerCase();
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style='margin: 0; padding: 0; background-color: #f3f4f6; font-family: Arial, sans-serif;'>
                    <div style='max-width: 600px; margin: 0 auto; background-color: #ffffff;'>
                        <!-- Header -->
                        <div style='background: linear-gradient(135deg, #0891b2 0%%, #06b6d4 100%%); padding: 40px 20px; text-align: center;'>
                            <div style='background-color: rgba(255,255,255,0.2); width: 80px; height: 80px; border-radius: 50%%; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center;'>
                                <span style='font-size: 40px;'>👤</span>
                            </div>
                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>Account Created</h1>
                            <p style='color: #cffafe; margin: 10px 0 0 0;'>Welcome to TDMS</p>
                        </div>
                        
                        <!-- Content -->
                        <div style='padding: 40px 30px;'>
                            <h2 style='color: #0891b2; margin: 0 0 20px 0; font-size: 22px;'>Your Account is Ready!</h2>
                            
                            <p style='color: #4b5563; line-height: 1.6; margin: 0 0 20px 0;'>
                                An account has been created for you in the Transport Data Management System.
                            </p>
                            
                            <!-- Credentials Box -->
                            <div style='background: linear-gradient(135deg, #ecfeff 0%%, #cffafe 100%%); border: 2px solid #06b6d4; padding: 25px; margin: 30px 0; border-radius: 12px;'>
                                <h3 style='color: #0891b2; margin: 0 0 20px 0; font-size: 18px; text-align: center;'>🔑 Account Details</h3>
                                
                                <div style='background-color: #ffffff; padding: 20px; border-radius: 8px; margin-bottom: 15px;'>
                                    <p style='color: #6b7280; margin: 0 0 5px 0; font-size: 14px;'>Email</p>
                                    <p style='color: #1f2937; margin: 0; font-size: 16px; font-weight: bold; word-break: break-all;'>%s</p>
                                </div>
                                
                                <div style='background-color: #ffffff; padding: 20px; border-radius: 8px; margin-bottom: 15px;'>
                                    <p style='color: #6b7280; margin: 0 0 5px 0; font-size: 14px;'>Temporary Password</p>
                                    <p style='color: #1f2937; margin: 0; font-size: 16px; font-weight: bold; font-family: monospace; letter-spacing: 1px;'>%s</p>
                                </div>
                                
                                <div style='background-color: #ffffff; padding: 20px; border-radius: 8px;'>
                                    <p style='color: #6b7280; margin: 0 0 5px 0; font-size: 14px;'>Your Role</p>
                                    <p style='color: #1f2937; margin: 0; font-size: 16px; font-weight: bold;'>%s</p>
                                </div>
                            </div>
                            
                            <!-- Next Steps -->
                            <div style='background-color: #dbeafe; border-left: 4px solid #3b82f6; padding: 20px; margin: 30px 0; border-radius: 4px;'>
                                <h3 style='color: #1e40af; margin: 0 0 15px 0; font-size: 16px;'>📋 Next Steps</h3>
                                <ol style='color: #1e3a8a; margin: 0; padding-left: 20px; line-height: 1.8; font-size: 14px;'>
                                    <li>Click the button below to log in</li>
                                    <li>Change your password (required on first login)</li>
                                    <li>Complete your profile information</li>
                                    <li>Start using the system</li>
                                </ol>
                            </div>
                            
                            <!-- CTA Button -->
                            <div style='text-align: center; margin: 30px 0;'>
                                <a href='http://localhost:3000/login' style='display: inline-block; background: linear-gradient(135deg, #0891b2 0%%, #06b6d4 100%%); color: #ffffff; text-decoration: none; padding: 14px 40px; border-radius: 8px; font-weight: bold; font-size: 16px;'>
                                    Login Now
                                </a>
                            </div>
                            
                            <p style='color: #6b7280; font-size: 14px; margin: 20px 0 0 0; text-align: center;'>
                                Keep your credentials secure and do not share them with anyone.
                            </p>
                        </div>
                        
                        <!-- Footer -->
                        <div style='background-color: #f9fafb; padding: 30px; text-align: center; border-top: 1px solid #e5e7eb;'>
                            <p style='color: #9ca3af; font-size: 14px; margin: 0;'>
                                © 2026 TDMS. All rights reserved.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """, to, password, roleName);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send user credentials email to {}: {}", to, e.getMessage());
            return false;
        }
    }

    public boolean sendPasswordResetEmail(String to, String resetToken, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("🔒 Password Reset Request - TDMS");
            
            String greeting = (firstName != null && !firstName.isEmpty()) ? firstName : "User";
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style='margin: 0; padding: 0; background-color: #f3f4f6; font-family: Arial, sans-serif;'>
                    <div style='max-width: 600px; margin: 0 auto; background-color: #ffffff;'>
                        <!-- Header -->
                        <div style='background: linear-gradient(135deg, #dc2626 0%%, #ef4444 100%%); padding: 40px 20px; text-align: center;'>
                            <div style='background-color: rgba(255,255,255,0.2); width: 80px; height: 80px; border-radius: 50%%; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center;'>
                                <span style='font-size: 40px;'>🔒</span>
                            </div>
                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>Password Reset</h1>
                            <p style='color: #fecaca; margin: 10px 0 0 0;'>Secure your account</p>
                        </div>
                        
                        <!-- Content -->
                        <div style='padding: 40px 30px;'>
                            <h2 style='color: #dc2626; margin: 0 0 20px 0; font-size: 22px;'>Hello, %s!</h2>
                            
                            <p style='color: #4b5563; line-height: 1.6; margin: 0 0 20px 0;'>
                                We received a request to reset your password for your TDMS account. If you didn't make this request, you can safely ignore this email.
                            </p>
                            
                            <!-- Info Box -->
                            <div style='background-color: #fef2f2; border-left: 4px solid #dc2626; padding: 20px; margin: 30px 0; border-radius: 4px;'>
                                <h3 style='color: #991b1b; margin: 0 0 10px 0; font-size: 16px;'>⏰ Important</h3>
                                <p style='color: #7f1d1d; margin: 0; font-size: 14px; line-height: 1.6;'>
                                    This password reset link will expire in <strong>24 hours</strong> for security reasons.
                                </p>
                            </div>
                            
                            <!-- CTA Button -->
                            <div style='text-align: center; margin: 30px 0;'>
                                <a href='http://localhost:3000/reset-password?token=%s' style='display: inline-block; background: linear-gradient(135deg, #dc2626 0%%, #ef4444 100%%); color: #ffffff; text-decoration: none; padding: 14px 40px; border-radius: 8px; font-weight: bold; font-size: 16px;'>
                                    Reset Password
                                </a>
                            </div>
                            
                            <!-- Security Tips -->
                            <div style='background-color: #eff6ff; border-left: 4px solid #3b82f6; padding: 20px; margin: 30px 0; border-radius: 4px;'>
                                <h3 style='color: #1e40af; margin: 0 0 15px 0; font-size: 16px;'>🛡️ Security Tips</h3>
                                <ul style='color: #1e3a8a; margin: 0; padding-left: 20px; line-height: 1.8; font-size: 14px;'>
                                    <li>Never share this reset link with anyone</li>
                                    <li>Create a strong, unique password</li>
                                    <li>Don't use passwords from other accounts</li>
                                    <li>Contact support if you didn't request this reset</li>
                                </ul>
                            </div>
                            
                            <p style='color: #6b7280; font-size: 14px; margin: 30px 0 0 0; text-align: center; line-height: 1.6;'>
                                If the button doesn't work, copy and paste this link into your browser:<br>
                                <span style='color: #3b82f6; word-break: break-all;'>http://localhost:3000/reset-password?token=%s</span>
                            </p>
                        </div>
                        
                        <!-- Footer -->
                        <div style='background-color: #f9fafb; padding: 30px; text-align: center; border-top: 1px solid #e5e7eb;'>
                            <p style='color: #9ca3af; font-size: 14px; margin: 0;'>
                                © 2026 TDMS. All rights reserved.
                            </p>
                            <p style='color: #9ca3af; font-size: 12px; margin: 10px 0 0 0;'>
                                This is an automated security message. Please do not reply.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """, greeting, resetToken, resetToken);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage());
            return false;
        }
    }

    public boolean sendEmailChangeNotification(String oldEmail, String newEmail, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            // Send to old email
            helper.setTo(oldEmail);
            helper.setSubject("⚠️ Email Address Changed - TDMS");
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style='margin: 0; padding: 0; background-color: #f3f4f6; font-family: Arial, sans-serif;'>
                    <div style='max-width: 600px; margin: 0 auto; background-color: #ffffff;'>
                        <!-- Header -->
                        <div style='background: linear-gradient(135deg, #f59e0b 0%%, #fbbf24 100%%); padding: 40px 20px; text-align: center;'>
                            <div style='background-color: rgba(255,255,255,0.2); width: 80px; height: 80px; border-radius: 50%%; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center;'>
                                <span style='font-size: 40px;'>⚠️</span>
                            </div>
                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>Security Alert</h1>
                            <p style='color: #fef3c7; margin: 10px 0 0 0;'>Email Address Changed</p>
                        </div>
                        
                        <!-- Content -->
                        <div style='padding: 40px 30px;'>
                            <h2 style='color: #f59e0b; margin: 0 0 20px 0; font-size: 22px;'>Hello, %s!</h2>
                            
                            <p style='color: #4b5563; line-height: 1.6; margin: 0 0 20px 0;'>
                                This is to notify you that your TDMS account email address has been changed.
                            </p>
                            
                            <!-- Change Details -->
                            <div style='background: linear-gradient(135deg, #fffbeb 0%%, #fef3c7 100%%); border: 2px solid #f59e0b; padding: 25px; margin: 30px 0; border-radius: 12px;'>
                                <h3 style='color: #92400e; margin: 0 0 20px 0; font-size: 18px; text-align: center;'>📧 Email Change Details</h3>
                                
                                <div style='background-color: #ffffff; padding: 20px; border-radius: 8px; margin-bottom: 15px;'>
                                    <p style='color: #6b7280; margin: 0 0 5px 0; font-size: 14px;'>Previous Email</p>
                                    <p style='color: #dc2626; margin: 0; font-size: 16px; font-weight: bold; text-decoration: line-through;'>%s</p>
                                </div>
                                
                                <div style='background-color: #ffffff; padding: 20px; border-radius: 8px;'>
                                    <p style='color: #6b7280; margin: 0 0 5px 0; font-size: 14px;'>New Email</p>
                                    <p style='color: #059669; margin: 0; font-size: 16px; font-weight: bold;'>%s</p>
                                </div>
                            </div>
                            
                            <!-- Warning Box -->
                            <div style='background-color: #fef2f2; border-left: 4px solid #dc2626; padding: 20px; margin: 30px 0; border-radius: 4px;'>
                                <h3 style='color: #991b1b; margin: 0 0 10px 0; font-size: 16px;'>🚨 Didn't make this change?</h3>
                                <p style='color: #7f1d1d; margin: 0 0 15px 0; font-size: 14px; line-height: 1.6;'>
                                    If you did not authorize this change, please contact our support team immediately to secure your account.
                                </p>
                                <a href='mailto:support@tdms.com' style='color: #dc2626; font-weight: bold; font-size: 14px;'>Contact Support →</a>
                            </div>
                        </div>
                        
                        <!-- Footer -->
                        <div style='background-color: #f9fafb; padding: 30px; text-align: center; border-top: 1px solid #e5e7eb;'>
                            <p style='color: #9ca3af; font-size: 14px; margin: 0;'>
                                © 2026 TDMS. All rights reserved.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """, firstName, oldEmail, newEmail);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            
            // Send confirmation to new email
            MimeMessage newMessage = mailSender.createMimeMessage();
            MimeMessageHelper newHelper = new MimeMessageHelper(newMessage, true);
            newHelper.setFrom(fromEmail);
            newHelper.setTo(newEmail);
            newHelper.setSubject("✅ Email Address Confirmed - TDMS");
            
            String newHtmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style='margin: 0; padding: 0; background-color: #f3f4f6; font-family: Arial, sans-serif;'>
                    <div style='max-width: 600px; margin: 0 auto; background-color: #ffffff;'>
                        <!-- Header -->
                        <div style='background: linear-gradient(135deg, #059669 0%%, #10b981 100%%); padding: 40px 20px; text-align: center;'>
                            <div style='background-color: rgba(255,255,255,0.2); width: 80px; height: 80px; border-radius: 50%%; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center;'>
                                <span style='font-size: 40px;'>✅</span>
                            </div>
                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>Email Confirmed</h1>
                            <p style='color: #d1fae5; margin: 10px 0 0 0;'>Successfully Updated</p>
                        </div>
                        
                        <!-- Content -->
                        <div style='padding: 40px 30px;'>
                            <h2 style='color: #059669; margin: 0 0 20px 0; font-size: 22px;'>Hello, %s!</h2>
                            
                            <p style='color: #4b5563; line-height: 1.6; margin: 0 0 20px 0;'>
                                This email confirms that your TDMS account email address has been successfully changed to this address.
                            </p>
                            
                            <!-- Success Box -->
                            <div style='background: linear-gradient(135deg, #ecfdf5 0%%, #d1fae5 100%%); border: 2px solid #10b981; padding: 25px; margin: 30px 0; border-radius: 12px; text-align: center;'>
                                <div style='font-size: 60px; margin-bottom: 15px;'>✉️</div>
                                <p style='color: #065f46; margin: 0; font-size: 18px; font-weight: bold;'>
                                    %s
                                </p>
                                <p style='color: #047857; margin: 10px 0 0 0; font-size: 14px;'>
                                    is now your login email
                                </p>
                            </div>
                            
                            <!-- Next Steps -->
                            <div style='background-color: #dbeafe; border-left: 4px solid #3b82f6; padding: 20px; margin: 30px 0; border-radius: 4px;'>
                                <h3 style='color: #1e40af; margin: 0 0 10px 0; font-size: 16px;'>📝 What's Next?</h3>
                                <p style='color: #1e3a8a; margin: 0; font-size: 14px; line-height: 1.6;'>
                                    You can now use this email address to log in to your TDMS account.
                                </p>
                            </div>
                            
                            <!-- CTA Button -->
                            <div style='text-align: center; margin: 30px 0;'>
                                <a href='http://localhost:3000/login' style='display: inline-block; background: linear-gradient(135deg, #059669 0%%, #10b981 100%%); color: #ffffff; text-decoration: none; padding: 14px 40px; border-radius: 8px; font-weight: bold; font-size: 16px;'>
                                    Login to TDMS
                                </a>
                            </div>
                        </div>
                        
                        <!-- Footer -->
                        <div style='background-color: #f9fafb; padding: 30px; text-align: center; border-top: 1px solid #e5e7eb;'>
                            <p style='color: #9ca3af; font-size: 14px; margin: 0;'>
                                © 2026 TDMS. All rights reserved.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """, firstName, newEmail);
            
            newHelper.setText(newHtmlContent, true);
            mailSender.send(newMessage);
            
            return true;
        } catch (Exception e) {
            log.error("Failed to send email change notification: {}", e.getMessage());
            return false;
        }
    }

    public boolean sendAccountDeletionEmail(String email, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("👋 Account Deleted - TDMS");
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style='margin: 0; padding: 0; background-color: #f3f4f6; font-family: Arial, sans-serif;'>
                    <div style='max-width: 600px; margin: 0 auto; background-color: #ffffff;'>
                        <!-- Header -->
                        <div style='background: linear-gradient(135deg, #6b7280 0%%, #9ca3af 100%%); padding: 40px 20px; text-align: center;'>
                            <div style='background-color: rgba(255,255,255,0.2); width: 80px; height: 80px; border-radius: 50%%; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center;'>
                                <span style='font-size: 40px;'>👋</span>
                            </div>
                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>Goodbye!</h1>
                            <p style='color: #e5e7eb; margin: 10px 0 0 0;'>Account Deleted</p>
                        </div>
                        
                        <!-- Content -->
                        <div style='padding: 40px 30px;'>
                            <h2 style='color: #4b5563; margin: 0 0 20px 0; font-size: 22px;'>Dear %s,</h2>
                            
                            <p style='color: #4b5563; line-height: 1.6; margin: 0 0 20px 0;'>
                                Your TDMS account has been successfully deleted as requested.
                            </p>
                            
                            <!-- Deletion Confirmation -->
                            <div style='background-color: #f3f4f6; border: 2px solid #9ca3af; padding: 25px; margin: 30px 0; border-radius: 12px; text-align: center;'>
                                <div style='font-size: 60px; margin-bottom: 15px;'>🗑️</div>
                                <p style='color: #1f2937; margin: 0; font-size: 18px; font-weight: bold;'>
                                    Account Permanently Deleted
                                </p>
                                <p style='color: #6b7280; margin: 10px 0 0 0; font-size: 14px;'>
                                    All your data has been removed from our system
                                </p>
                            </div>
                            
                            <!-- Thank You Message -->
                            <div style='background-color: #eff6ff; border-left: 4px solid #3b82f6; padding: 20px; margin: 30px 0; border-radius: 4px;'>
                                <p style='color: #1e40af; margin: 0; font-size: 16px; line-height: 1.6;'>
                                    <strong>Thank you for using TDMS!</strong><br><br>
                                    We're sorry to see you go. If you change your mind, you can always create a new account in the future.
                                </p>
                            </div>
                            
                            <p style='color: #6b7280; font-size: 14px; margin: 30px 0 0 0; text-align: center; line-height: 1.6;'>
                                If you have any feedback about your experience, we'd love to hear from you.
                            </p>
                        </div>
                        
                        <!-- Footer -->
                        <div style='background-color: #f9fafb; padding: 30px; text-align: center; border-top: 1px solid #e5e7eb;'>
                            <p style='color: #9ca3af; font-size: 14px; margin: 0;'>
                                © 2026 TDMS. All rights reserved.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """, firstName);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send account deletion email to {}: {}", email, e.getMessage());
            return false;
        }
    }
    
    public boolean sendNegativeFeedbackAlert(
        String adminEmail, 
        String adminName,
        Long feedbackId,
        String customerInfo,
        Integer rating,
        String category,
        String feedbackText,
        String bookingReference
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("⚠️ URGENT: Negative Customer Feedback Received");
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style='margin: 0; padding: 0; background-color: #f3f4f6; font-family: Arial, sans-serif;'>
                    <div style='max-width: 600px; margin: 0 auto; background-color: #ffffff;'>
                        <!-- Header -->
                        <div style='background: linear-gradient(135deg, #dc2626 0%%, #b91c1c 100%%); padding: 40px 20px; text-align: center; border-top: 6px solid #991b1b;'>
                            <div style='background-color: rgba(255,255,255,0.15); width: 90px; height: 90px; border-radius: 50%%; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center; border: 3px solid rgba(255,255,255,0.3);'>
                                <span style='font-size: 50px;'>⚠️</span>
                            </div>
                            <h1 style='color: #ffffff; margin: 0; font-size: 30px; font-weight: bold; text-shadow: 0 2px 4px rgba(0,0,0,0.2);'>URGENT ALERT</h1>
                            <p style='color: #fecaca; margin: 10px 0 0 0; font-size: 16px;'>Negative Customer Feedback</p>
                        </div>
                        
                        <!-- Content -->
                        <div style='padding: 40px 30px;'>
                            <h2 style='color: #dc2626; margin: 0 0 10px 0; font-size: 22px;'>Dear %s,</h2>
                            <p style='color: #dc2626; font-weight: bold; font-size: 16px; margin: 0 0 25px 0;'>
                                A customer has submitted negative feedback that requires immediate attention.
                            </p>
                            
                            <!-- Feedback Details Box -->
                            <div style='background: linear-gradient(135deg, #fef2f2 0%%, #fee2e2 100%%); border: 3px solid #dc2626; padding: 25px; margin: 30px 0; border-radius: 12px; box-shadow: 0 4px 6px rgba(220,38,38,0.1);'>
                                <h3 style='margin: 0 0 20px 0; color: #991b1b; font-size: 20px; border-bottom: 2px solid #fca5a5; padding-bottom: 10px;'>📋 Feedback Details</h3>
                                
                                <div style='background-color: #ffffff; padding: 15px; border-radius: 8px; margin-bottom: 15px; border-left: 4px solid #dc2626;'>
                                    <p style='color: #6b7280; margin: 0 0 5px 0; font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px;'>Feedback ID</p>
                                    <p style='color: #dc2626; margin: 0; font-size: 18px; font-weight: bold; font-family: monospace;'>#%d</p>
                                </div>
                                
                                <div style='background-color: #ffffff; padding: 15px; border-radius: 8px; margin-bottom: 15px;'>
                                    <p style='color: #6b7280; margin: 0 0 5px 0; font-size: 12px; text-transform: uppercase;'>Customer</p>
                                    <p style='color: #1f2937; margin: 0; font-size: 16px; font-weight: bold;'>%s</p>
                                </div>
                                
                                <div style='background-color: #ffffff; padding: 15px; border-radius: 8px; margin-bottom: 15px;'>
                                    <p style='color: #6b7280; margin: 0 0 8px 0; font-size: 12px; text-transform: uppercase;'>Rating</p>
                                    <div style='font-size: 24px; line-height: 1;'>%s</div>
                                </div>
                                
                                <div style='background-color: #ffffff; padding: 15px; border-radius: 8px; margin-bottom: 15px;'>
                                    <p style='color: #6b7280; margin: 0 0 5px 0; font-size: 12px; text-transform: uppercase;'>Category</p>
                                    <p style='color: #1f2937; margin: 0; font-size: 16px; font-weight: 600;'>%s</p>
                                </div>
                                
                                %s
                                
                                <div style='background-color: #ffffff; padding: 20px; border-radius: 8px; border: 2px dashed #fca5a5;'>
                                    <p style='color: #6b7280; margin: 0 0 10px 0; font-size: 12px; text-transform: uppercase;'>Customer Feedback</p>
                                    <p style='color: #1f2937; margin: 0; font-size: 15px; line-height: 1.6; font-style: italic;'>
                                        "%s"
                                    </p>
                                </div>
                            </div>
                            
                            <!-- Action Required Box -->
                            <div style='background: linear-gradient(135deg, #dbeafe 0%%, #bfdbfe 100%%); border-left: 4px solid #3b82f6; padding: 20px; margin: 30px 0; border-radius: 8px;'>
                                <p style='margin: 0 0 10px 0; color: #1e40af; font-size: 16px;'><strong>📌 Action Required</strong></p>
                                <p style='margin: 0; color: #1e3a8a; font-size: 14px; line-height: 1.6;'>
                                    Please review this feedback and respond to the customer as soon as possible to maintain service quality and customer satisfaction.
                                </p>
                            </div>
                            
                            <!-- CTA Button -->
                            <div style='text-align: center; margin: 30px 0;'>
                                <a href='http://localhost:3000/dashboard' style='display: inline-block; background: linear-gradient(135deg, #dc2626 0%%, #b91c1c 100%%); color: #ffffff; text-decoration: none; padding: 16px 45px; border-radius: 8px; font-weight: bold; font-size: 16px; box-shadow: 0 4px 6px rgba(220,38,38,0.3);'>
                                    Review Feedback Now
                                </a>
                            </div>
                        </div>
                        
                        <!-- Footer -->
                        <div style='background-color: #f9fafb; padding: 30px; text-align: center; border-top: 1px solid #e5e7eb;'>
                            <p style='color: #9ca3af; font-size: 14px; margin: 0;'>
                                © 2026 TDMS Feedback System. All rights reserved.
                            </p>
                            <p style='color: #9ca3af; font-size: 12px; margin: 10px 0 0 0;'>
                                This is an automated alert. Please do not reply to this email.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                adminName,
                feedbackId,
                customerInfo,
                "★".repeat(rating) + "<span style='color: #d1d5db;'>" + "★".repeat(5 - rating) + "</span>",
                category,
                bookingReference != null ? String.format("<div style='background-color: #ffffff; padding: 15px; border-radius: 8px; margin-bottom: 15px;'><p style='color: #6b7280; margin: 0 0 5px 0; font-size: 12px; text-transform: uppercase;'>Booking Reference</p><p style='color: #1f2937; margin: 0; font-size: 16px; font-weight: 600; font-family: monospace;'>%s</p></div>", bookingReference) : "",
                feedbackText != null ? feedbackText : "No comments provided"
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send negative feedback alert: {}", e.getMessage());
            return false;
        }
    }

    public boolean sendFeedbackResponseNotification(
        String customerEmail,
        String customerName,
        Long feedbackId,
        String adminResponse
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(customerEmail);
            helper.setSubject("✉️ Response to Your Feedback - TDMS");
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style='margin: 0; padding: 0; background-color: #f3f4f6; font-family: Arial, sans-serif;'>
                    <div style='max-width: 600px; margin: 0 auto; background-color: #ffffff;'>
                        <!-- Header -->
                        <div style='background: linear-gradient(135deg, #1e40af 0%%, #3b82f6 100%%); padding: 40px 20px; text-align: center;'>
                            <div style='background-color: rgba(255,255,255,0.2); width: 80px; height: 80px; border-radius: 50%%; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center;'>
                                <span style='font-size: 40px;'>💬</span>
                            </div>
                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>Thank You!</h1>
                            <p style='color: #dbeafe; margin: 10px 0 0 0;'>We've responded to your feedback</p>
                        </div>
                        
                        <!-- Content -->
                        <div style='padding: 40px 30px;'>
                            <h2 style='color: #1e40af; margin: 0 0 20px 0; font-size: 22px;'>Dear %s,</h2>
                            
                            <p style='color: #4b5563; line-height: 1.6; margin: 0 0 25px 0;'>
                                Thank you for taking the time to share your feedback with us. We've carefully reviewed your comments and wanted to respond directly.
                            </p>
                            
                            <!-- Feedback Reference -->
                            <div style='background: linear-gradient(135deg, #eff6ff 0%%, #dbeafe 100%%); border: 2px solid #3b82f6; padding: 25px; margin: 30px 0; border-radius: 12px;'>
                                <h3 style='color: #1e40af; margin: 0 0 20px 0; font-size: 18px; border-bottom: 2px solid #93c5fd; padding-bottom: 10px;'>📋 Feedback Reference</h3>
                                
                                <div style='background-color: #ffffff; padding: 15px; border-radius: 8px; margin-bottom: 20px;'>
                                    <p style='color: #6b7280; margin: 0 0 5px 0; font-size: 12px; text-transform: uppercase;'>Feedback ID</p>
                                    <p style='color: #1e40af; margin: 0; font-size: 18px; font-weight: bold; font-family: monospace;'>#%d</p>
                                </div>
                                
                                <div style='background-color: #ffffff; padding: 20px; border-radius: 8px; border-left: 4px solid #3b82f6;'>
                                    <p style='color: #6b7280; margin: 0 0 12px 0; font-size: 12px; text-transform: uppercase; font-weight: 600;'>Our Response</p>
                                    <p style='color: #1f2937; margin: 0; font-size: 15px; line-height: 1.7;'>
                                        %s
                                    </p>
                                </div>
                            </div>
                            
                            <!-- Appreciation Box -->
                            <div style='background-color: #ecfdf5; border-left: 4px solid #10b981; padding: 20px; margin: 30px 0; border-radius: 4px;'>
                                <p style='color: #065f46; margin: 0; font-size: 16px; line-height: 1.6;'>
                                    <strong>Your feedback matters!</strong><br><br>
                                    Your input helps us continuously improve our service and provide better experiences for all our customers.
                                </p>
                            </div>
                            
                            <p style='color: #6b7280; font-size: 14px; margin: 30px 0 0 0; line-height: 1.6;'>
                                If you have any additional questions or concerns, please don't hesitate to contact us or submit new feedback.
                            </p>
                            
                            <!-- CTA Button -->
                            <div style='text-align: center; margin: 30px 0;'>
                                <a href='http://localhost:3000/login' style='display: inline-block; background: linear-gradient(135deg, #1e40af 0%%, #3b82f6 100%%); color: #ffffff; text-decoration: none; padding: 14px 40px; border-radius: 8px; font-weight: bold; font-size: 16px;'>
                                    View in TDMS
                                </a>
                            </div>
                        </div>
                        
                        <!-- Footer -->
                        <div style='background-color: #f9fafb; padding: 30px; text-align: center; border-top: 1px solid #e5e7eb;'>
                            <p style='color: #9ca3af; font-size: 14px; margin: 0;'>
                                © 2026 TDMS. All rights reserved.
                            </p>
                            <p style='color: #9ca3af; font-size: 12px; margin: 10px 0 0 0;'>
                                Thank you for choosing TDMS - Transport Data Management System
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                customerName,
                feedbackId,
                adminResponse
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send feedback response notification: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Send credentials email to newly registered driver
     * Profile is pre-completed, only password change required
     */
    public boolean sendDriverCredentialsEmail(String to, String driverName, String password) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Welcome to TDMS - Driver Portal Access");

            String emailContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                        .credentials { background: white; padding: 20px; border-left: 4px solid #667eea; margin: 20px 0; }
                        .credential-item { margin: 10px 0; }
                        .label { font-weight: bold; color: #667eea; }
                        .value { font-family: monospace; background: #f0f0f0; padding: 5px 10px; border-radius: 4px; }
                        .button { display: inline-block; background: #667eea; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                        .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                        .success { background: #d4edda; border-left: 4px solid #28a745; padding: 15px; margin: 20px 0; }
                        .steps { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; }
                        .step { display: flex; gap: 15px; margin: 15px 0; padding: 15px; background: #f8f9fa; border-radius: 8px; }
                        .step-number { background: #667eea; color: white; width: 30px; height: 30px; border-radius: 50%%; display: flex; align-items: center; justify-content: center; font-weight: bold; flex-shrink: 0; }
                        .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🚗 Welcome to TDMS</h1>
                            <p>Driver Portal Access</p>
                        </div>
                        <div class="content">
                            <p>Dear <strong>%s</strong>,</p>
                            
                            <p>Welcome to the Transport Driver Management System (TDMS)! Your driver account has been successfully created and is ready to use.</p>
                            
                            <div class="success">
                                <strong>✅ Good News!</strong><br>
                                Your profile has been pre-configured with your information. You only need to set your own password on first login.
                            </div>
                            
                            <div class="credentials">
                                <h3>Your Login Credentials:</h3>
                                <div class="credential-item">
                                    <span class="label">Email:</span><br>
                                    <span class="value">%s</span>
                                </div>
                                <div class="credential-item">
                                    <span class="label">Temporary Password:</span><br>
                                    <span class="value">%s</span>
                                </div>
                            </div>
                            
                            <div class="steps">
                                <h3>📋 First Time Login Steps:</h3>
                                
                                <div class="step">
                                    <div class="step-number">1</div>
                                    <div>
                                        <strong>Login with your credentials</strong><br>
                                        <span style="color: #666;">Use the email and temporary password above to access the driver portal.</span>
                                    </div>
                                </div>
                                
                                <div class="step">
                                    <div class="step-number">2</div>
                                    <div>
                                        <strong>Change your password</strong><br>
                                        <span style="color: #666;">You'll be prompted to create a new secure password (minimum 8 characters).</span>
                                    </div>
                                </div>
                                
                                <div class="step">
                                    <div class="step-number">3</div>
                                    <div>
                                        <strong>Access your dashboard</strong><br>
                                        <span style="color: #666;">After changing your password, you'll immediately access your driver dashboard!</span>
                                    </div>
                                </div>
                            </div>
                            
                            <div class="warning">
                                <strong>⚠️ Security Reminder:</strong>
                                <ul style="margin: 10px 0; padding-left: 20px;">
                                    <li>Change your password immediately after first login</li>
                                    <li>Choose a strong password with at least 8 characters</li>
                                    <li>Never share your password with anyone</li>
                                    <li>This temporary password will expire after first use</li>
                                </ul>
                            </div>
                            
                            <h3>What You Can Do in the Driver Portal:</h3>
                            <ul>
                                <li>📊 <strong>Dashboard:</strong> View your statistics, assigned vehicle, and today's schedule</li>
                                <li>📅 <strong>My Schedule:</strong> Check upcoming trips and manage your calendar</li>
                                <li>✅ <strong>Daily Checks:</strong> Submit pre-trip and post-trip vehicle inspections</li>
                                <li>⚠️ <strong>Report Issues:</strong> Report vehicle problems or incidents immediately</li>
                                <li>📋 <strong>Trip History:</strong> Access your complete trip records</li>
                                <li>📈 <strong>Performance:</strong> Track your performance metrics and ratings</li>
                            </ul>
                            
                            <center>
                                <a href="http://localhost:3000" class="button">Access Driver Portal Now</a>
                            </center>
                            
                            <h3>Need Help?</h3>
                            <p>If you have any questions or issues accessing your account:</p>
                            <ul>
                                <li>📞 Contact your manager or supervisor</li>
                                <li>📧 Email: support@tdms.com</li>
                                <li>☎️ Phone: +250 XXX XXX XXX</li>
                            </ul>
                            
                            <div style="background: #e3f2fd; padding: 15px; border-radius: 8px; margin-top: 20px;">
                                <p style="margin: 0;"><strong>💡 Pro Tip:</strong> Bookmark the driver portal URL for quick access!</p>
                            </div>
                            
                            <div class="footer">
                                <p>This is an automated email from TDMS. Please do not reply to this email.</p>
                                <p>&copy; 2024 Transport Driver Management System. All rights reserved.</p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """, driverName, to, password);

            helper.setText(emailContent, true);
            mailSender.send(message);
            
            log.info("Driver credentials email sent successfully to: {}", to);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to send driver credentials email to {}: {}", to, e.getMessage());
            return false;
        }
    }
    
}