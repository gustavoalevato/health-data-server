package br.com.zontec.watchmonitor.server.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sleep_sessions")
public class SleepSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;
    
    private LocalDate sleepDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer totalDurationMinutes;
    private LocalDateTime timestamp;
    
    @OneToMany(mappedBy = "sleepSession", cascade = CascadeType.ALL)
    private List<SleepEntry> entries;
    
}

