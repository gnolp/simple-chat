package com.example.blogforum.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.blogforum.DTO.FriendRequestDto;
import com.example.blogforum.DTO.ApiResponse;
import com.example.blogforum.DTO.UserDTO;
import com.example.blogforum.model.FriendRequest;
import com.example.blogforum.model.User;
import com.example.blogforum.model.Enum.FriendRequestStatus;
import com.example.blogforum.repository.FriendRequestRepository;
import com.example.blogforum.repository.UserRepository;
import com.example.blogforum.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;
	private final FriendRequestRepository friendRequestRepository;
	private final ChatService chatService;
	private final HelperService helper;
	public List<UserDTO> getAllUsers(){
		List<User> users = userRepository.findAll();
		return users.stream()
				.map(user-> UserDTO.builder()
						.id(user.getId())
						.image(user.getImage())
	                    .name(user.getName())     
	                    .email(user.getEmail())    
	                    .role(user.getRole())
	                    .dob(user.getDob())
	                    .build())
				.collect(Collectors.toList());
	}
	@Transactional
	public ResponseEntity<ApiResponse> updateProfile(Long id, UserDTO request, UserPrincipal userPrincipal) {
		Long loggedInUserId = userPrincipal.getUserId();
		if (!loggedInUserId.equals(id)) {
	        return ResponseEntity.status(HttpStatus.FORBIDDEN)
	                .body(new ApiResponse(false, "You can only update your own profile"));
	    }
		Optional<User> optionalUser = userRepository.findById(id);
		if(!optionalUser.isPresent()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                .body(new ApiResponse(false, "User not found"));
		}
		User user = optionalUser.get();
		user.setName(request.getName());
		user.setDob(request.getDob());
		userRepository.save(user);
		return ResponseEntity.ok(new ApiResponse(true, "Profile updated successfully"));
	}
	public ResponseEntity<ApiResponse> sendRequest(Long fromUserId, Long toUserId) {
		User userSend = userRepository.findById(fromUserId).get();
		User userReceived = userRepository.findById(toUserId).get();
		Optional<FriendRequest> optionalReqeust = friendRequestRepository.findBySenderAndReceiver(userSend, userReceived);
		if(!optionalReqeust.isPresent()) {
			FriendRequest re = new FriendRequest();
			re.setReceiver(userReceived);
			re.setSender(userSend);
			re.setStatus(FriendRequestStatus.PENDING);
			friendRequestRepository.save(re);
			return ResponseEntity.ok(new ApiResponse(true,"Đã gửi lời mời kết bạn!"));
		}
		return ResponseEntity.badRequest().body(new ApiResponse(false,"Có lỗi xảy ra khi gửi lời mời kết bạn!"));
	}
	public void acceptFriendRequest(Long requestId) {
	    FriendRequest request = friendRequestRepository.findById(requestId)
	        .orElseThrow(() -> new RuntimeException("Lời mời không tồn tại"));

	    if (request.getStatus() == FriendRequestStatus.PENDING) {
	        request.setStatus(FriendRequestStatus.ACCEPTED);
	        friendRequestRepository.save(request);

	        User sender = request.getSender();
	        User receiver = request.getReceiver();

	        sender.getFriends().add(receiver);
	        receiver.getFriends().add(sender);

	        userRepository.save(sender);
	        userRepository.save(receiver);
	        chatService.findOrCreateChat(sender.getId(), receiver.getId());
	    }
	}
	public ResponseEntity<List<FriendRequestDto>> getFriendRequests(Long userId) {
	    User user = userRepository.findById(userId)
	            .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại!"));

	    List<FriendRequest> requests = friendRequestRepository.findByReceiverAndStatus(user, FriendRequestStatus.PENDING);
	    List<FriendRequestDto> requestDtos = requests.stream()
	            .map(request -> new FriendRequestDto(request.getId(), request.getSender().getId(), request.getSender().getName()))
	            .collect(Collectors.toList());
	    return ResponseEntity.ok(requestDtos);
	}
	

	public List<UserDTO> getListFriend(Long id){
		List<User> friends = userRepository.findAllFriendsById(id);
		return friends.stream().map(user-> UserDTO.builder()
				.name(user.getName())     
                .email(user.getEmail())    
                .role(user.getRole())
                .dob(user.getDob())
                .build())
				.collect(Collectors.toList());
	}
	public UserDTO getMe() {
		Long userId = helper.getCurrentUserId();
		if(!userId.equals(null)) {
			Optional<User> temp = userRepository.findById(userId);
			if(!temp.isEmpty()) {
				User user = temp.get();
				return UserDTO.builder()
				.email(user.getEmail())
				.id(userId)
				.image(user.getImage())
				.name(user.getName())
				.build();
			}
		}
		return null;
	}

}
