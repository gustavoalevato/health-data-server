package br.com.zontec.watchmonitor.server.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Table(name = "oxygen_levels")
public class Oxygen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "type", nullable = false)
    private Integer type;  // Tipo de medição de oxigênio (ex: 0 para SpO2, 1 para outro tipo)

    @Column(name = "oxygen_level", nullable = false)
    private Integer oxygenLevel;  // Nível de oxigênio em %

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

   
}