package com.example.blogforum.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor 
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	private final HttpHandshakeInterceptor handshakeInterceptor;
  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic","/user");
    config.setApplicationDestinationPrefixes("/app");
    config.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
	  registry.addEndpoint("/chat").setAllowedOriginPatterns("http://127.0.0.1:3000").addInterceptors(handshakeInterceptor).withSockJS(); // thêm interceptor
	  //registry.addEndpoint("/chat").; // Endpoint kết nối WebSocket
  }

}