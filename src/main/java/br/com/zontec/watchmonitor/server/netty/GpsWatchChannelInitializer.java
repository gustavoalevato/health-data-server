package br.com.zontec.watchmonitor.server.netty;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

@Component // Torna este um bean do Spring
public class GpsWatchChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Autowired
    private GpsWatchProtocolHandlerFactory handlerFactory; // Injeta a factory de handlers

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // Decodificador para lidar com o delimitador '#'
        // O GPS Watch protocol_2023.docx indica que o terminador é '#'.
        // Max frame length é o tamanho máximo de uma mensagem antes do '#'
        pipeline.addLast("hybridFrameDecoder", new GpsWatchHybridFrameDecoder(65535));

        // ISO-8859-1 preserva 1 byte por caractere, o que evita corromper payloads binarios TK.
        pipeline.addLast("decoder", new StringDecoder(StandardCharsets.ISO_8859_1));

        // Comandos de saída continuam ASCII, mas ISO-8859-1 também preserva payloads binarios quando necessario.
        pipeline.addLast("encoder", new StringEncoder(StandardCharsets.ISO_8859_1));

        // Nosso handler de lógica de negócio para o protocolo.
        // A factory garante que cada canal obtenha uma nova instância de handler,
        // com suas dependências Spring injetadas.
        pipeline.addLast("handler", handlerFactory.createHandler());
    }
}
