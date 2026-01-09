package backend.tdms.com.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import backend.tdms.com.dto.UpdateEmailRequest;
import backend.tdms.com.dto.UpdateProfileRequest;
import backend.tdms.com.dto.DeleteAccountRequest;
import backend.tdms.com.dto.UserSettingsResponse;
import backend.tdms.com.service.SettingsService;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired
    private SettingsService settingsService;

    @GetMapping
    public ResponseEntity<UserSettingsResponse> getSettings() {
        return ResponseEntity.ok(settingsService.getUserSettings());
    }

    @PostMapping("/update-email")
    public ResponseEntity<String> updateEmail(@RequestBody UpdateEmailRequest request) {
        settingsService.updateEmail(request);
        return ResponseEntity.ok("Email updated successfully");
    }

    @PostMapping("/update-profile")
    public ResponseEntity<String> updateProfile(@RequestBody UpdateProfileRequest request) {
        settingsService.updateProfile(request);
        return ResponseEntity.ok("Profile updated successfully");
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<String> deleteAccount(@RequestBody DeleteAccountRequest request) {
        settingsService.deleteAccount(request.getPassword());
        return ResponseEntity.ok("Account deleted successfully");
    }
}