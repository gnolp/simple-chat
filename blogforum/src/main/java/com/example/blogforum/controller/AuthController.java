package com.example.blogforum.controller;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.blogforum.DTO.ApiResponse;
import com.example.blogforum.DTO.UserDTO;
import com.example.blogforum.model.LoginRequest;
import com.example.blogforum.model.LoginResponse;
import com.example.blogforum.model.RegisterRequest;
import com.example.blogforum.model.User;
import com.example.blogforum.repository.UserRepository;
import com.example.blogforum.security.JwtIssuer;
import com.example.blogforum.security.JwtDecoder;
import com.example.blogforum.security.JwtToPrincipalConverter;
import com.example.blogforum.security.UserPrincipal;
import com.example.blogforum.service.UserService;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@OpenAPIDefinition(
	    info = @Info(title = "User API", version = "1.0", description = "Quản lý người dùng")
	)
public class AuthController {
	private final JwtIssuer jwtIssuer;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserService userService;
	private final JwtDecoder jwtDecoder;
	private final JwtToPrincipalConverter jwtToPrincipalConverter;
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody @Validated LoginRequest request, HttpServletResponse response) {
	    Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
	    
	    if (optionalUser.isEmpty()) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                .body(new ApiResponse(false, "Email không đúng!"));
	    }

	    User user = optionalUser.get();
	    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                .body(new ApiResponse(false, "Mật khẩu không đúng!"));
	    }
	    System.out.println(user.getEmail() + " " + user.getName());
	    String accessToken = jwtIssuer.issue(user.getId(), user.getEmail(), List.of(user.getRole()));
	    String refreshToken = jwtIssuer.refeshToken(user.getId(), user.getEmail(), List.of(user.getRole()));

	    // Tạo cookie chứa access token
	    ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", accessToken)
	            .httpOnly(true)  // Ngăn JS truy cập
	            .secure(true)    // Chỉ gửi qua HTTPS (nên bật nếu dùng HTTPS)
	            .path("/")       // Đường dẫn áp dụng cookie
	            .maxAge(Duration.ofMinutes(15))
	            .sameSite("None") // CSRF protection
	            .build();

	    // Có thể thêm cả refresh_token nếu cần
	    ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", refreshToken)
	            .httpOnly(true)
	            .secure(true) // de true khi dung https hoac khi deploys
	            .path("/")
	            .maxAge(Duration.ofMinutes(15))
	            .sameSite("None")
	            .build();
//	    ResponseCookie clearAccess = ResponseCookie.from("access_token", "")
//	            .path("/")
//	            .maxAge(0)
//	            .build();
//
//	    ResponseCookie clearRefresh = ResponseCookie.from("refresh_token", "")
//	            .path("/")
//	            .maxAge(0)
//	            .build();
//
//	    response.addHeader(HttpHeaders.SET_COOKIE, clearAccess.toString());
//	    response.addHeader(HttpHeaders.SET_COOKIE, clearRefresh.toString());
//	    // Gắn vào response header
	    response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
	    response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

	    // Trả về thông tin user nếu cần
	    return ResponseEntity.ok(ApiResponse.builder().success(true).message("Login done!").build());
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse> register(@Validated @RequestBody RegisterRequest request) {
	    if (userRepository.existsByEmail(request.getEmail())) {
	        return ResponseEntity.badRequest().body(new ApiResponse(false, "Email had used!"));
	    }
	    try {
	        User user = new User();
	        user.setEmail(request.getEmail());
	        user.setPassword(passwordEncoder.encode(request.getPassword()));
	        user.setRole("User");
	        userRepository.save(user);
	        return ResponseEntity.ok(new ApiResponse(true, "Register successful"));
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(new ApiResponse(false, "Errors, please try again!"));
	    }
	}
	@PutMapping("/update-profile/{id}")
	public ResponseEntity<ApiResponse> changeInfo(@RequestBody UserDTO request,@PathVariable Long id,@AuthenticationPrincipal UserPrincipal userPrincipal){
		return userService.updateProfile(id, request, userPrincipal);
	}
	@PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@CookieValue(name = "refresh_token", required =false) String refreshToken) {
        
		if (refreshToken == null || refreshToken.isEmpty()) {
	        return ResponseEntity.badRequest().body(new ApiResponse(false, "Missing refresh token"));
	    }
        try {
            var decoded = jwtDecoder.decodeRefreshToken(refreshToken);
            var principal = jwtToPrincipalConverter.convert(decoded);
            Long userId = principal.getUserId();
            String email = principal.getEmail();
            // Extract roles as strings
            var authorities = principal.getAuthorities();
            List<String> roles = authorities.stream().map(Object::toString).toList();
            String newAccessToken = jwtIssuer.issue(userId, email, roles);
            ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", newAccessToken)
            		.httpOnly(true)
            		.secure(true)
            		.path("/")
            		.sameSite("None")
            		.build();
            return ResponseEntity.ok().header(HttpHeaders.COOKIE, accessTokenCookie.toString()).body("");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse(false, "Invalid or expired refresh token"));
        }
    }
	@GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        UserDTO user = userService.getMe();
        return ResponseEntity.ok(user);
    }
}
