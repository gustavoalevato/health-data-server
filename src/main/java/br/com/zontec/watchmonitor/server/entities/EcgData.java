package br.com.zontec.watchmonitor.server.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
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
@Table(name = "ecg_data")
public class EcgData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;
    
    private LocalDateTime ecgTimestamp;
    // Outros campos
    @Column(columnDefinition = "JSON")
    private String dataPoints;  // JSON array de inteiros
    
    private Integer totalPackets;   // Número total de pacotes para este ECG
    private Integer currentPacket;  // Número do pacote atual
    private BigDecimal gain;           // Ganho (ex: 200, 400)
    private BigDecimal zeroVoltage;    // Voltagem zero
    private Integer leadType;       // Tipo de derivação (ex: 0: 1-lead)
    private Integer samplingRate;   // Taxa de amostragem
    private List<String> ecgDataPoints; // Os dados brutos do ECG
    
    private LocalDateTime timestamp;
    
}