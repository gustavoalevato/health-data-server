package br.com.zontec.watchmonitor.server.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Touch4PAudioStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPersistInboundAmrFileOnDisk() throws IOException {
        Touch4PAudioStorageService service = new Touch4PAudioStorageService(tempDir.toString());
        byte[] audio = new byte[] { 'A', 'M', 'R', '\n' };

        Path savedFile = service.saveInboundAudio("8800000015", audio);

        assertTrue(Files.exists(savedFile));
        assertTrue(savedFile.getFileName().toString().startsWith("8800000015_"));
        assertTrue(savedFile.getFileName().toString().endsWith(".amr"));
        assertArrayEquals(audio, Files.readAllBytes(savedFile));
    }
}
