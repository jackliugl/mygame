package com.lgl.service;

import com.lgl.model.BarcodeMessage;
import com.lgl.model.BarcodeMessage.BarcodeItem;
import com.lgl.service.impl.BarCodeServiceImpl;
import lombok.Data;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface BarCodeService extends MyWebSocketService {

	static Map<String, BarcodeItem> currentItemMap = new LinkedHashMap<String, BarcodeItem>();

	public String checkBarcode(String barcode);

	public void clearAll();

	public void removeItem(BarcodeItem item);

	public void updateItem(BarcodeItem item);

	void leave(String sessionId);

	void startService();
	void stopService();

	SimpMessagingTemplate simpMessagingTemplate = null;

	public void setSimpMessagingTemplate(SimpMessagingTemplate simpMessagingTemplate);

	public SimpMessagingTemplate getSimpMessagingTemplate();

	BarcodeMessage processBarcodeMessage(String sessionID, BarcodeMessage message);

	String verifyPin(String pin);

	List<BarCodeServiceImpl.GiftCard> getAllCards();

	String changePIN(String cardNo, String oldPin, String newPin);

	String transferBalance(String cardNo1, String pin, String cardNo2, double amount);
}
