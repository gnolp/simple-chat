package com.example.blogforum.security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtDecoder jwtDecoder;
	private final JwtToPrincipalConverter jwtToPrincipalConverter;
	@Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/login")
                || path.equals("/register")
                || path.equals("/refresh-token");
    }
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		extractTokenFromRequest(request)
			.map(jwtDecoder::decode)
			.map(jwtToPrincipalConverter::convert)
			.map(UserPrincipalAuthenticationToken::new)
			.ifPresent(authentication -> SecurityContextHolder.getContext().setAuthentication(authentication));
		filterChain.doFilter(request, response);
	}

//	private Optional<String> extractTokenFromRequest(HttpServletRequest request){
//		var token  = request.getHeader("Authorization");
//		if(StringUtils.hasText(token) && token.startsWith("Bearer ")) {
//			return Optional.of(token.substring(7));
//		}
//		return Optional.empty();
//	}
	private Optional<String> extractTokenFromRequest(HttpServletRequest request) {
	    // Lấy danh sách cookie từ request
		System.out.println("dang loc cookie");
	    if (request.getCookies() != null) {
	        for (var cookie : request.getCookies()) {
	            if ("access_token".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
	            	System.out.println(cookie.getValue());
	                return Optional.of(cookie.getValue());
	            }
	        }
	    }
	    return Optional.empty();
	}

}
