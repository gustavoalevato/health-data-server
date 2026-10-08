package br.com.zontec.watchmonitor.server.netty;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;

import br.com.zontec.watchmonitor.server.utils.Delimiters;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class Touch4PChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Autowired
    private GpsWatchProtocolHandlerFactory handlerFactory; // Injeta a factory de handlers

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // Decodificador para lidar com o delimitador '#'
        // O GPS Watch protocol_2023.docx indica que o terminador é '#'.
        // Max frame length é o tamanho máximo de uma mensagem antes do '#'
        pipeline.addLast("framer", new DelimiterBasedFrameDecoder(65535, Delimiters.hash("]")));

        pipeline.addLast("decoder", new StringDecoder(StandardCharsets.ISO_8859_1));

        pipeline.addLast("encoder", new StringEncoder(StandardCharsets.ISO_8859_1));

        // Nosso handler de lógica de negócio para o protocolo.
        // A factory garante que cada canal obtenha uma nova instância de handler,
        // com suas dependências Spring injetadas.
        pipeline.addLast("handler", handlerFactory.createHandler());
    }
}
