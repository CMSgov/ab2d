package gov.cms.ab2d.hpms.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient buildWebClient() {
        final int timeout = 90;
        TcpClient tcpClient = TcpClient.newConnection();
        HttpClient httpClient = HttpClient.from(tcpClient)
                .tcpConfiguration(client -> client.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout * 1000)
                        .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(timeout))));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
