package com.lgl.controller;

import com.lgl.model.BarcodeMessage;
import com.lgl.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.RequestContextHolder;

import com.lgl.model.CanvasMessage;
import com.lgl.model.ChatMessage;
import com.lgl.model.GameMessage;

import java.util.HashMap;
import java.util.Map;

@Controller
public class MyWebSocketController {
	
	@Autowired 
	SimpMessagingTemplate simpMessagingTemplate;
	
	@Autowired
	MyChatService myChatService;
	
	@Autowired
	MyCanvasService myCanvasService;

	@Autowired
	MyGameService myGameService;

	@Autowired
	BarCodeService barcodeService;

	@MessageMapping("/hello")
    @SendTo("/topic/messages")
    public ChatMessage greeting(@Header("simpSessionId") String sessionID, ChatMessage message) throws Exception {
    	
    	if (myChatService.getSimpMessagingTemplate() == null) {
    		myChatService.setSimpMessagingTemplate(simpMessagingTemplate);
    	}

		MyWebSocketService.sessionServiceMap.putIfAbsent(sessionID, myChatService);

    	// manually send message
    	//simpMessagingTemplate.convertAndSend("/topic/messages", "Good afternoon from jack!");
    	
        //Thread.sleep(1000); // simulated delay
    	
        ChatMessage respMessage = myChatService.processChatMessage(sessionID, message);
        		
        // return value will be sent to @SentTo destination
        //return new ChatMessage("Hello " + message.getName() + "!");
    	return respMessage;
    }
    
    // this is for QR scan login - topic for the specific client session
    @MessageMapping("/hello/{sessionid}")
    @SendTo("/topic/messages/{sessionid}")
    public String greetingQR(String message, @DestinationVariable ("sessionid") String sessionID) throws Exception {
        return "Hello from QR server, your JSESSIONID is: " + sessionID;
    }
    
    // This is for drawing canvas
    @MessageMapping("/canvas")
    @SendTo("/topic/canvas")
    public CanvasMessage myCanvas(@Header("simpSessionId") String sessionID, CanvasMessage message) throws Exception {
    	
    	if (myCanvasService.getSimpMessagingTemplate() == null) {
    		myCanvasService.setSimpMessagingTemplate(simpMessagingTemplate);
    	}

		MyWebSocketService.sessionServiceMap.putIfAbsent(sessionID, myCanvasService);
    	CanvasMessage respMessage = myCanvasService.processCanvasMessage(sessionID, message);
        		
    	return respMessage;
    }
    
    // This is for drawing game
    @MessageMapping("/gameCanvas")
    @SendTo("/topic/gameCanvas")
    public CanvasMessage myGameCanvas(@Header("simpSessionId") String sessionID, CanvasMessage message) throws Exception {
    	
    	if (myGameService.getSimpMessagingTemplate() == null) {
    		myGameService.setSimpMessagingTemplate(simpMessagingTemplate);
    	}

		MyWebSocketService.sessionServiceMap.putIfAbsent(sessionID, myGameService);
    	CanvasMessage respMessage = myGameService.processCanvasMessage(sessionID, message);
        		
    	return respMessage;
    }
    
    @MessageMapping("/gameStatus")
    @SendTo("/topic/gameStatus")
    public GameMessage myGameStatus(@Header("simpSessionId") String sessionID, GameMessage message) throws Exception {
    	
    	if (myGameService.getSimpMessagingTemplate() == null) {
    		myGameService.setSimpMessagingTemplate(simpMessagingTemplate);
    	}

		MyWebSocketService.sessionServiceMap.putIfAbsent(sessionID, myGameService);
    	GameMessage respMessage = myGameService.processGameMessage(sessionID, message);
        		
    	return respMessage;
    }

	@MessageMapping("/barcodeStatus")
	@SendTo("/topic/barcodeStatus")
	public BarcodeMessage myBarcodeStatus(@Header("simpSessionId") String sessionID, BarcodeMessage message) throws Exception {

		if (barcodeService.getSimpMessagingTemplate() == null) {
			barcodeService.setSimpMessagingTemplate(simpMessagingTemplate);
		}

		MyWebSocketService.sessionServiceMap.putIfAbsent(sessionID, barcodeService);
		BarcodeMessage respMessage = barcodeService.processBarcodeMessage(sessionID, message);

		return respMessage;
	}
}
