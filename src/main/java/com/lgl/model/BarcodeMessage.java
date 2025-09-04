package com.lgl.model;

import java.util.List;

import com.lgl.util.Utils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class BarcodeMessage {

	private String sessionID;

	// start scan request
	private boolean started = false;

	// for client -> server
	private boolean clearAll;
	private boolean deleteFlag;
	private BarcodeItem barcodeItem;

	// for server -> clients
	private List<BarcodeItem> allItems = null;
	private boolean transactionCompleted = false;

	private String result;

	// client -> server only
	private boolean paymentStarted = false;
	private boolean cancelPayment = false;
	private boolean confirmPayment = false;
	private boolean startOver = false;

	private PaymentInfo paymentInfo;

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class BarcodeItem {
		private String barcode;
		private int quantity;
		private double price;
		private double amount;
		private boolean fake = false;

		public BarcodeItem(String barcode) {
			this.barcode = barcode;
			this.quantity = 0;
			this.price = 0;
			this.amount = 0;
			this.fake = false;
		}

		public void add(Double price) {
			this.quantity++;
			this.price = price;
			this.amount = Utils.formatDouble(this.quantity * price);
		}

		public void updateQuantity(int quantity) {
			this.quantity = quantity;
			this.amount = Utils.formatDouble(this.quantity * this.price);
		}
	}

	@Data
	public static class PaymentInfo {
		private String cardNo;
		private double cardBalance;
		private double totalCost;
		private double newBalance;
		private boolean pinProtected = false;
		private boolean pinVerified = false;
	}
}
