package com.example.blogforum.security;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor
public class JwtIssuer {
	// tạo mã jwt cho user với thông tin là id, email, danh sách roles, token có giá trị 1 ngày, 
	private final JwtProperties properties;
	public String issue(long userId, String email, List<String> roles) {
		return JWT.create()
				.withSubject(String.valueOf(userId))
				.withExpiresAt(Instant.now().plus(Duration.of(60,ChronoUnit.MINUTES)))
				.withClaim("e", email)
				.withClaim("a", roles)
				.sign(Algorithm.HMAC256(properties.getSecretKey()));
	}
	public String refeshToken(Long userId,String email,List<String> roles) {
		return JWT.create()
				.withSubject(String.valueOf(userId))
				.withExpiresAt(Instant.now().plus(Duration.of(7,ChronoUnit.DAYS)))
				.withClaim("e", email)
				.withClaim("a", roles)
				.sign(Algorithm.HMAC256(properties.getSecretKeyRefesh()));
	}
}
