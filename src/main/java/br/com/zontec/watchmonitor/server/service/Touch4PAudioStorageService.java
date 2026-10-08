package br.com.zontec.watchmonitor.server.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class Touch4PAudioStorageService {

    private static final DateTimeFormatter FILE_NAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final Path storageDirectory;

    public Touch4PAudioStorageService(@Value("${touch4p.audio.storage-directory:./data/touch4p/audio}") String storageDirectory) {
        this.storageDirectory = Paths.get(storageDirectory).toAbsolutePath().normalize();
    }

    public Path saveInboundAudio(String imei, byte[] audioData) {
        if (imei == null || imei.isBlank()) {
            throw new IllegalArgumentException("IMEI nao informado para gravacao do audio TK.");
        }
        if (audioData == null || audioData.length == 0) {
            throw new IllegalArgumentException("Audio TK vazio.");
        }

        String filename = imei + "_" + LocalDateTime.now().format(FILE_NAME_TIMESTAMP) + ".amr";
        Path target = storageDirectory.resolve(filename);

        try {
            Files.createDirectories(storageDirectory);
            return Files.write(target, audioData);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar audio TK em disco.", e);
        }
    }

    Path storageDirectory() {
        return storageDirectory;
    }
}
