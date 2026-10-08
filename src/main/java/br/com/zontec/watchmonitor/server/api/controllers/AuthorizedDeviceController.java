package br.com.zontec.watchmonitor.server.api.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.zontec.watchmonitor.server.entities.Device;
import br.com.zontec.watchmonitor.server.enums.DeviceStatus;
import br.com.zontec.watchmonitor.server.enums.ParserModel;
import br.com.zontec.watchmonitor.server.repositories.DeviceRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/authorized-devices")
@RequiredArgsConstructor
public class AuthorizedDeviceController {

    @Autowired
    private DeviceRepository deviceRepository;

    // GET /api/devices - Listar todos os dispositivos
    @GetMapping
    public ResponseEntity<List<Device>> getAllDevices() {
        List<Device> devices = deviceRepository.findByStatus(DeviceStatus.ACTIVE);
        return ResponseEntity.ok(devices);
    }

    // GET /api/devices/{id} - Buscar dispositivo por ID
    @GetMapping("/{id}")
    public ResponseEntity<Device> getDeviceById(@PathVariable Long id) {
        Optional<Device> device = deviceRepository.findById(id);
        if (device.isPresent()) {
            return ResponseEntity.ok(device.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/imei/{imei}")
    public ResponseEntity<Device> getDeviceByImei(@PathVariable String imei) {
        Optional<Device> device = deviceRepository.findByImei(imei);
        if (device.isPresent()) {
            return ResponseEntity.ok(device.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // POST /api/devices - Criar um novo dispositivo
    @PostMapping
    public ResponseEntity<Device> createDevice(@RequestBody Device device) {
        // Definir timestamps se não fornecidos
        if (device.getCreatedAt() == null) {
            device.setCreatedAt(LocalDateTime.now());
        }
        if (device.getUpdatedAt() == null) {
            device.setUpdatedAt(LocalDateTime.now());
        }
        if (device.getParserModel() == null || device.getParserModel().isBlank()) {
            device.setParserModel(ParserModel.GPS_WATCH.name());
        } else {
            device.setParserModel(device.getParserModel().trim().toUpperCase());
        }
        device.setStatus(DeviceStatus.ACTIVE);
        Device savedDevice = deviceRepository.save(device);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDevice);
    }

    // PUT /api/devices/{id} - Atualizar um dispositivo existente
    @PutMapping("/{id}")
    public ResponseEntity<Device> updateDevice(@PathVariable Long id, @RequestBody Device deviceDetails) {
        Optional<Device> optionalDevice = deviceRepository.findById(id);
        if (optionalDevice.isPresent()) {
            Device device = optionalDevice.get();
            // Atualizar campos
            device.setImei(deviceDetails.getImei());
            device.setStatus(deviceDetails.getStatus());
            device.setLastSeen(deviceDetails.getLastSeen());
            if (deviceDetails.getParserModel() != null && !deviceDetails.getParserModel().isBlank()) {
                device.setParserModel(deviceDetails.getParserModel().trim().toUpperCase());
            }
            device.setUpdatedAt(LocalDateTime.now());
            Device updatedDevice = deviceRepository.save(device);
            return ResponseEntity.ok(updatedDevice);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE /api/devices/{id} - Deletar um dispositivo
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        Optional<Device> device = deviceRepository.findById(id);
        if (device.isPresent()) {
            deviceRepository.delete(device.get());
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
