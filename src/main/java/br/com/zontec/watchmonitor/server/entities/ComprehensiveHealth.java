package br.com.zontec.watchmonitor.server.entities;

import java.math.BigDecimal;
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
@Table(name = "comprehensive_health")
public class ComprehensiveHealth {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;
    
    private Integer heartRate;
    private Integer  systolicBp;  // Pressão Arterial Sistólica
    private Integer  diastolicBp; // Pressão Arterial Diastólica
    private Integer  spo2;                   // Saturação de Oxigênio no Sangue
    private BigDecimal bloodSugar;     // Açúcar no Sangue (pode ser decimal)
    private BigDecimal bodyTemperature;
    
    private LocalDateTime timestamp;
    
}