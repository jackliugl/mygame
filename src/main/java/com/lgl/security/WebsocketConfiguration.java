package com.lgl.security;

import com.lgl.service.BarCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import com.lgl.service.MyCanvasService;
import com.lgl.service.MyChatService;
import com.lgl.service.MyGameService;

@Configuration
@EnableWebSocketMessageBroker
public class WebsocketConfiguration implements WebSocketMessageBrokerConfigurer {
	
	@Autowired
	MyChatService myChatService;

	@Autowired
	MyCanvasService myCanvasService;

	@Autowired
	MyGameService myGameService;

    @Autowired
    BarCodeService barcodeService;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
        	.setAllowedOrigins("https://192.168.0.14:8082", "https://192.168.0.14:8443", "wss://192.168.0.14:8443", "http://localhost:8082", "http://192.168.0.14:8082", "https://mygame-6zac4xqw4q-pd.a.run.app", "http://mygame-6zac4xqw4q-pd.a.run.app:8080", "chrome-extension://pfdhoblngboilpfeibdedpjgfnlcodoo")
        	.withSockJS();
    }
 
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new MyChannelInterceptor(myChatService, myCanvasService, myGameService, barcodeService));
    }

	@Override
	public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
		registry.setMessageSizeLimit(512*1024);
		registry.setSendBufferSizeLimit(2*1024*1024);
		registry.setSendTimeLimit(20*1000);
	}
    
	/*
	 *  Note: below code fix the 1009 error
	 *  websocket close 1009 "The decoded text message was too big for the output buffer 
	 *  and the endpoint does not support partial messages
	 */
	@Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // set bufferSize here
        container.setMaxTextMessageBufferSize(512000);
        container.setMaxBinaryMessageBufferSize(512000);
        container.setMaxSessionIdleTimeout(15 * 60000L);
        return container;
    }	
}
