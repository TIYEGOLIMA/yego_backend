package com.yego.backend.config;

import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Avoids noisy Tomcat acceptor errors on some local/JDK socket combinations
 * when SO_LINGER is applied to newly accepted sockets.
 */
@Configuration
public class TomcatSocketConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatSocketCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            connector.setProperty("connectionLinger", "-1");
            connector.setProperty("socket.soLingerOn", "false");
            connector.setProperty("socket.soLingerTime", "-1");

            ProtocolHandler protocolHandler = connector.getProtocolHandler();
            if (protocolHandler instanceof AbstractProtocol<?> protocol) {
                protocol.setConnectionLinger(-1);
            }
        });
    }
}
