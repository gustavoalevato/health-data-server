package br.com.zontec.watchmonitor.server.service;

import java.nio.file.Path;

import org.springframework.core.io.Resource;

public record AudioStreamPayload(
        String fileName,
        Resource resource,
        long contentLength,
        Path path
) {
}
