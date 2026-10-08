package br.com.zontec.watchmonitor.server.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GpsWatchTcpServer {

    private static final Logger log = LoggerFactory.getLogger(GpsWatchTcpServer.class);

    @Value("${netty.tcp.port:5088}") // Porta TCP, padrão 5088
    private int port;

    @Autowired
    private GpsWatchChannelInitializer channelInitializer; // Injeta nosso inicializador de canais

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    @PostConstruct // Será executado após a inicialização do bean Spring
    public void start() throws InterruptedException {
        log.info("Starting Netty TCP server on port {}", port);

        bossGroup = new NioEventLoopGroup(); // Grupo de threads para aceitar novas conexões
        workerGroup = new NioEventLoopGroup(); // Grupo de threads para lidar com a I/O das conexões aceitas

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class) // Usando um canal NIO para comunicação assíncrona
             .childHandler(channelInitializer) // Nosso pipeline de handlers Netty
             .option(ChannelOption.SO_BACKLOG, 128) // Número máximo de conexões pendentes na fila
             .childOption(ChannelOption.SO_KEEPALIVE, true); // Manter a conexão viva

            // Bind e inicia o servidor.
            channelFuture = b.bind(port).sync();
            log.info("Netty TCP server started successfully on port {}", port);

        } catch (InterruptedException e) {
            log.error("Failed to start Netty TCP server", e);
            Thread.currentThread().interrupt(); // Restaura o status de interrupção
        }
    }

    @PreDestroy // Será executado antes da destruição do bean Spring
    public void stop() {
        log.info("Shutting down Netty TCP server...");
        if (channelFuture != null && channelFuture.channel().isOpen()) {
            try {
                channelFuture.channel().close().sync(); // Fecha o canal
            } catch (InterruptedException e) {
                log.error("Error closing Netty channel", e);
                Thread.currentThread().interrupt();
            }
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(); // Desliga os grupos de threads
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        log.info("Netty TCP server shut down.");
    }
}