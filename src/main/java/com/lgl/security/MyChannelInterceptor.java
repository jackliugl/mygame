package com.lgl.security;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.lgl.service.*;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;

public class MyChannelInterceptor implements ChannelInterceptor  {
	
	MyChatService myChatService;
	MyCanvasService myCanvasService;
	MyGameService myGameService;
	BarCodeService barCodeService;
	
    public MyChannelInterceptor(MyChatService myChatService, MyCanvasService myCanvasService, MyGameService myGameService, BarCodeService barcodeService) {
		this.myChatService = myChatService;
		this.myCanvasService = myCanvasService;
		this.myGameService = myGameService;
		this.barCodeService = barcodeService;
	}

	@Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        //System.out.println(" ### preSend Command: " + command + ", message: " + message);

        if (command == StompCommand.DISCONNECT) {
        	// TODO: call corresponding leave() method ONLY based on accessor.getSessionId()

			String sessionId = accessor.getSessionId();
			MyWebSocketService service = MyWebSocketService.sessionServiceMap.get(sessionId);
			if (service != null) {
				System.out.println("Disconnected for session: " + sessionId + ", service type: " + service.getClass());
				service.leave(sessionId);
				MyWebSocketService.sessionServiceMap.remove(sessionId);
			}

        	/*
        	myChatService.leave(accessor.getSessionId());
        	myCanvasService.leave(accessor.getSessionId());
        	myGameService.leave(accessor.getSessionId());
        	barCodeService.leave(accessor.getSessionId());
        	 */
        	return null;
        }
        
        return message;
    }
}
