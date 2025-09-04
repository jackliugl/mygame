package com.lgl.controller;

import com.lgl.security.LoggedInSession;
import com.lgl.service.BarCodeService;
import com.lgl.service.impl.BarCodeServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
public class BarCodeController {

	@Autowired 
	SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
	private BarCodeService barcodeService;

	@GetMapping("/barcode/scan/{barcode}")
	public ResponseEntity<String> scanBarCode(@PathVariable("barcode") String barcode) {
		String result = barcodeService.checkBarcode(barcode);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@GetMapping("/barcode/verifyPin/{pin}")
	public ResponseEntity<String> verifyPin(@PathVariable("pin") String pin) {
		String result = barcodeService.verifyPin(pin);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@GetMapping("/barcode/card/all")
	public ResponseEntity<List<BarCodeServiceImpl.GiftCard>> getAllCards() {
		return new ResponseEntity<>(barcodeService.getAllCards(), HttpStatus.OK);
	}

	@GetMapping("/barcode/card/changePin/{cardNo}:{oldPin}:{newPin}")
	public ResponseEntity<String> changePIN(@PathVariable("cardNo") String cardNo, @PathVariable("oldPin") String oldPin, @PathVariable("newPin") String newPin) {
		return new ResponseEntity<>(barcodeService.changePIN(cardNo, oldPin, newPin), HttpStatus.OK);
	}

	@GetMapping("/barcode/card/transfer/{cardNo1}:{pin}:{cardNo2}:{amount}")
	public ResponseEntity<String> transfer(@PathVariable("cardNo1") String cardNo1, @PathVariable("pin") String pin,
										   @PathVariable("cardNo2") String cardNo2, @PathVariable("amount") double amount) {
		return new ResponseEntity<>(barcodeService.transferBalance(cardNo1, pin, cardNo2, amount), HttpStatus.OK);
	}

	@GetMapping("/barcode/sessionid")
	public ResponseEntity<String> getCurrentSessionId() {

		String sessionID = RequestContextHolder.currentRequestAttributes().getSessionId();

		return new ResponseEntity<>(sessionID, HttpStatus.OK);
	}
}
