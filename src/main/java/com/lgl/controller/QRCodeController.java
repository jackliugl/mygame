package com.lgl.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;

import com.lgl.model.Tutorial;
import com.lgl.security.LoggedInSession;
import com.lgl.service.QRCodeService;

@RestController
public class QRCodeController {

	@Autowired 
	SimpMessagingTemplate simpMessagingTemplate;

	@Autowired 
	LoggedInSession loggedInSession;
	
	private final int WIDTH = 250;
	private final int HEIGHT = 250;

	@Autowired
	private QRCodeService qrCodeService;

	@GetMapping("/qr-code")
	public ResponseEntity<byte[]> getQrCode() {
		String sessionID = RequestContextHolder.currentRequestAttributes().getSessionId();
		String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
		
		byte[] qrImage = qrCodeService.generate(sessionID + "-"+ dateStr, WIDTH, HEIGHT);

		return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(qrImage);
	}
	
	@GetMapping("/qr-code/sessionid")
	public ResponseEntity<String> getCurrentSessionId() {
		
		String sessionID = RequestContextHolder.currentRequestAttributes().getSessionId();
		
		return new ResponseEntity<>(sessionID, HttpStatus.OK);
	}
	
	@GetMapping("/qr-code/login/{sessionid}")
	public ResponseEntity<String> qrLogin(@PathVariable("sessionid") String qrSessionID) {
		
		String userName = loggedInSession.userLoggedInByQRCode(qrSessionID);
		
		// notify websocket client of success login
		simpMessagingTemplate.convertAndSend("/topic/messages/" + qrSessionID, "Login Success:" + userName);
		
		return new ResponseEntity<>(userName, HttpStatus.OK);
	}
	
	@GetMapping("/qr-code/loginStatus")
	public ResponseEntity<String> loginStatus() {
		
		if (loggedInSession.checkIfQRLoggedIn()) {
			return new ResponseEntity<>("OK", HttpStatus.OK);
		}
		
		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}
}
