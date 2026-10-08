package br.com.zontec.watchmonitor.server.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "heartbeats")
public class Heartbeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;
    
    private Integer batteryPercentage;
    private Integer gsmStrength;
    
    private Integer numSatellites;
    private Integer steps;
    private Integer spaceLeft;
    private Integer rollsFrequency;
    
    private String status;
    
    private LocalDateTime timestamp;
    
}