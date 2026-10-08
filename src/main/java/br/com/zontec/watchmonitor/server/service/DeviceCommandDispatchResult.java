package br.com.zontec.watchmonitor.server.service;

public record DeviceCommandDispatchResult(
        boolean sent,
        String sentFrame
) {
}
