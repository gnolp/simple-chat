package com.example.blogforum.service;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.blogforum.DTO.UserDTO;
import com.example.blogforum.model.User;
import com.example.blogforum.repository.UserRepository;
import com.example.blogforum.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HelperService {
	private final UserRepository userRepository;
	public Long getCurrentUserId() {
		
	    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//	    System.out.println("Authentication: " + authentication);
//	    System.out.println("Is Authenticated: " + authentication.isAuthenticated());
//	    System.out.println("Principal class: " + authentication.getPrincipal().getClass());
//	    System.out.println("Principal: " + authentication.getPrincipal());
	    if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
	        return null;
	    }

	    if (authentication.getPrincipal() instanceof UserPrincipal) {
	        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
//	        System.out.println("User ID: " + currentUser.getUserId());
	        return currentUser.getUserId();
	    }
	    
	    return null;
	}
	public boolean isCurrentUserId(Long id) {
		Long curentID = getCurrentUserId();
		return curentID == id;
	}
	public UserDTO getCurrentUserDTO() {
		Optional<User> a = userRepository.findById(getCurrentUserId());
		User user = a.get();
		return UserDTO.builder()
				.dob(user.getDob())
				.name(user.getName())
				.image(user.getImage())
				.email(user.getEmail())
				.role(user.getRole())
				.id(user.getId())
				.build();
	}
}
