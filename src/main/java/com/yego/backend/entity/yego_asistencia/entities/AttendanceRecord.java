package com.yego.backend.entity.yego_asistencia.entities;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "module_attendance_records")
public class AttendanceRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_type", nullable = false)
    private AttendanceType attendanceType;
    
    @Column(name = "recorded_date", nullable = false)
    private LocalDate recordedDate;
    
    @Column(name = "recorded_time", nullable = false)
    private LocalTime recordedTime;
    
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
    
    @Column(name = "public_ip", columnDefinition = "inet")
    private String publicIp;
    
    @Column(name = "local_ip", columnDefinition = "inet")
    private String localIp;
    
    @Column(name = "computer_name")
    private String computerName;
    
    @Column(name = "windows_username")
    private String windowsUsername;
    
    @Column(name = "machine_id")
    private String machineId;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "browser_name")
    private String browserName;
    
    @Column(name = "browser_version")
    private String browserVersion;
    
    @Column(name = "operating_system")
    private String operatingSystem;
    
    @Column(name = "timezone")
    private String timezone;
    
    @Column(name = "screen_resolution")
    private String screenResolution;
    
    @Column(name = "language")
    private String language;
    
    @Column(name = "platform")
    private String platform;
    
    @Column(name = "is_manual")
    private Boolean isManual = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public AttendanceRecord() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public AttendanceRecord(Long userId, AttendanceType attendanceType) {
        this();
        this.userId = userId;
        this.attendanceType = attendanceType;
        this.recordedDate = LocalDate.now();
        this.recordedTime = LocalTime.now();
        this.recordedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public AttendanceType getAttendanceType() {
        return attendanceType;
    }
    
    public void setAttendanceType(AttendanceType attendanceType) {
        this.attendanceType = attendanceType;
    }
    
    public LocalDate getRecordedDate() {
        return recordedDate;
    }
    
    public void setRecordedDate(LocalDate recordedDate) {
        this.recordedDate = recordedDate;
    }
    
    public LocalTime getRecordedTime() {
        return recordedTime;
    }
    
    public void setRecordedTime(LocalTime recordedTime) {
        this.recordedTime = recordedTime;
    }
    
    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
    
    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
    
    public String getPublicIp() {
        return publicIp;
    }
    
    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }
    
    public String getLocalIp() {
        return localIp;
    }
    
    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }
    
    public String getComputerName() {
        return computerName;
    }
    
    public void setComputerName(String computerName) {
        this.computerName = computerName;
    }
    
    public String getWindowsUsername() {
        return windowsUsername;
    }
    
    public void setWindowsUsername(String windowsUsername) {
        this.windowsUsername = windowsUsername;
    }
    
    public String getMachineId() {
        return machineId;
    }
    
    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getBrowserName() {
        return browserName;
    }
    
    public void setBrowserName(String browserName) {
        this.browserName = browserName;
    }
    
    public String getBrowserVersion() {
        return browserVersion;
    }
    
    public void setBrowserVersion(String browserVersion) {
        this.browserVersion = browserVersion;
    }
    
    public String getOperatingSystem() {
        return operatingSystem;
    }
    
    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    public String getScreenResolution() {
        return screenResolution;
    }
    
    public void setScreenResolution(String screenResolution) {
        this.screenResolution = screenResolution;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getPlatform() {
        return platform;
    }
    
    public void setPlatform(String platform) {
        this.platform = platform;
    }
    
    public Boolean getIsManual() {
        return isManual;
    }
    
    public void setIsManual(Boolean isManual) {
        this.isManual = isManual;
    }
    
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
