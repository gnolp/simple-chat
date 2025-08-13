package com.example.blogforum.controller;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.blogforum.DTO.ApiResponse;
import com.example.blogforum.DTO.ChatDTO;
import com.example.blogforum.DTO.MessageDTO;
import com.example.blogforum.model.Chat;
import com.example.blogforum.security.UserPrincipal;
import com.example.blogforum.service.ChatService;
import com.example.blogforum.service.CloudinaryService;
import com.example.blogforum.service.HelperService;
import com.example.blogforum.service.ImageChatService;
import com.example.blogforum.service.MessageService;
import com.example.blogforum.service.UserChatService;
import com.example.blogforum.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ChatController {
	private final ChatService chatService;
	private final HelperService helperService;
	private final UserChatService userChatService;
	private final MessageService messageService;
	private final CloudinaryService cloudinaryService;
	private final ImageChatService imgChatService;
	@MessageMapping("/chat/{chatId}")
	public ResponseEntity<?> sendMessage(@DestinationVariable Long chatId,
			@Payload Map<String,String> message,Message<?> headers) {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(headers);
	    UserPrincipal principal = (UserPrincipal) accessor.getSessionAttributes().get("userPrincipal");
	    System.out.println("AUTH in /send-message: " + principal.getUserId());
		System.out.println("📩 Received message: " + message);
		MessageDTO msg = messageService.convertDTO(message,principal.getUserId()); 
		if(msg==null) return ResponseEntity.badRequest().body(new ApiResponse(false,"Vui long nhap noi dung tin nhan"));
		return chatService.send(msg,chatId);
	}
	
	@GetMapping("/get-list-chat")
	public ResponseEntity<?> getChats(@RequestParam(defaultValue = "0") int page) {
	    Long userId = helperService.getCurrentUserId();

	    if (userId == null) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                .body(new ApiResponse(false, "Người dùng chưa đăng nhập"));
	    }

	    List<ChatDTO> chats = chatService.getChatsByUserId(userId,page);

	    if (chats.isEmpty()) {
	        return ResponseEntity.ok(new ApiResponse(true, "Không có tin nhắn nào"));
	    }

	    return ResponseEntity.ok(chats);
	}
	@GetMapping("/get/chatnoDTO")
	public ResponseEntity<?> getChatsnoDTO() {
	    Long userId = helperService.getCurrentUserId();

	    if (userId == null) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                .body(new ApiResponse(false, "Người dùng chưa đăng nhập"));
	    }

	    List<Chat> chats = chatService.getChatsByUserIdNoDTO(userId);

	    if (chats.isEmpty()) {
	        return ResponseEntity.ok(new ApiResponse(true, "Không có tin nhắn nào"));
	    }
	    return ResponseEntity.ok(chats);
	}
	@GetMapping("/get-message-of-chat/{chatId}")
	@PreAuthorize("@chatService.isIn(#chatId)")
	public ResponseEntity<List<MessageDTO>> getMsg(
	        @PathVariable Long chatId,
	        @RequestParam(defaultValue = "0") int page // lấy page từ query param
	) {
		System.out.println("hello?");
	    return messageService.getMessByChatId(chatId, page,helperService.getCurrentUserId());
	}
	@PutMapping("/create-group-chat")
	public ResponseEntity<?> creatGroupChat(@RequestBody List<Long> memberId){
		Long userId = helperService.getCurrentUserId();
		return chatService.createGroupChat(userId, memberId);
	}
	
	@PutMapping("/change-image-of-group/{chatId}")
	@PreAuthorize("@chatService.isIn(#chatId)")
	public ResponseEntity<?> changeImageGroup(@PathVariable Long chatId, MultipartFile file){
		String url;
		try {
			url = cloudinaryService.uploadFileChat(file);
		}catch (IOException e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed");
	    }
		chatService.changeImage(chatId, url);
		return ResponseEntity.ok(new ApiResponse(true,"Done!"));
	}
	@DeleteMapping("/delete-user-of-chat/{chatId}")
	@PreAuthorize("@userChatService.role(#chatId).equals('admin')")
	public ResponseEntity<?> deleteUserFromChat(@PathVariable Long chatId, @RequestParam Long userId){
		userChatService.deleteUserFromChat(chatId,userId);
		return ResponseEntity.ok(new ApiResponse(true,"done"));
	}
	@PutMapping("/add-user-into-chat/{chatId}")
	@PreAuthorize("@userChatService.role(#chatId).equals('admin')")
	public ResponseEntity<?> addUserIntoChat(@PathVariable Long chatId, @RequestParam Long userId){
		userChatService.addUserFromChat(chatId,userId);
		return ResponseEntity.ok(new ApiResponse(true,"done"));
	}
	
	@PatchMapping("change-name-of-chat/{chatId}")
	@PreAuthorize("@chatService.isIn(#chatId)")
	public ResponseEntity<?> changeNameInChat(@PathVariable Long chatId, @RequestBody Map<Long,String> user_name){
		userChatService.changeNameInChat(chatId,Long.parseLong(user_name.get("id")),user_name.get("name"));
		return ResponseEntity.ok(new ApiResponse(true,"done"));
	}
	@PostMapping("/upload-file/{chatId}")
	public ResponseEntity<?> uploadFile(@PathVariable Long chatId, MultipartFile file ){
		try {
	        String url = cloudinaryService.uploadFileChat(file);
	        System.out.println(url);
	        Map<String, String> response = new HashMap<>();
	        response.put("url", url);
	        imgChatService.save(url,chatId);
	        return ResponseEntity.ok(response);
	    } catch (IOException e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed");
	    }
	}
}
