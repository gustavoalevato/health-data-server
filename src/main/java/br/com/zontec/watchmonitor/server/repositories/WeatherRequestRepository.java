package br.com.zontec.watchmonitor.server.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.zontec.watchmonitor.server.entities.Device;
import br.com.zontec.watchmonitor.server.entities.WeatherRequest;

@Repository
public interface WeatherRequestRepository extends JpaRepository<WeatherRequest, Long> {

    // Buscar solicitações de clima por dispositivo
    List<WeatherRequest> findByDevice(Device device);
}