package com.lgl.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.Gson;
import com.lgl.model.BarcodeMessage;
import com.lgl.service.BarCodeService;
import com.lgl.util.Utils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.glxn.qrgen.javase.QRCode;
import org.apache.commons.lang.StringUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Data
@Service
public class BarCodeServiceImpl implements BarCodeService {

    public static Map<String, Double> barcodePriceMap;

    public static GiftCardMap giftcardMap = new GiftCardMap();

    AtomicBoolean dataChanged = new AtomicBoolean(false);

    boolean started = false;
    SimpMessagingTemplate simpMessagingTemplate = null;
    Timer timer = null;

    boolean paymentStarted = false;
    BarcodeMessage.PaymentInfo paymentInfo = new BarcodeMessage.PaymentInfo();

    boolean transactionCompleted = false;

    Gson gson = new Gson();

    static {
        barcodePriceMap = new HashMap<String, Double>();
        barcodePriceMap.put("APPLE", 1.29);
        barcodePriceMap.put("GRAPE", 1.49);
        barcodePriceMap.put("TOMATO", 0.67);
        barcodePriceMap.put("MILK", 5.99);
        barcodePriceMap.put("CHEESE", 1.99);
        barcodePriceMap.put("CHERRY", 3.88);
        barcodePriceMap.put("GREEN PEPPER", 1.79);

        barcodePriceMap.put("CORN", 0.12);
        barcodePriceMap.put("BLUEBERRY", 2.56);
        barcodePriceMap.put("STRAWBERRY", 2.88);
        barcodePriceMap.put("LEMON", 0.29);
    }

    // set default balance
    static {
        giftcardMap.put("Claire001", new GiftCard("Claire001", 100.0, ""));
        giftcardMap.put("Helen001", new GiftCard("Helen001", 100.0, ""));
        giftcardMap.put("Jack001", new GiftCard("Jack001", 100.0, "111"));
        giftcardMap.put("Erica001", new GiftCard("Erica001", 100.0, ""));
    }

    public static class GiftCardMap extends HashMap<String, GiftCard> {
    }

    @Data
    @AllArgsConstructor
    public static class GiftCard {
        private String cardNo;
        private Double balance;

        //@JsonIgnore
        private String pin;
    }

    public BarCodeServiceImpl() {
        loadGiftCardMap();
    }

    private void loadGiftCardMap() {
        try (Reader reader = new FileReader("/tmp/balance.json")) {
            Map<String, GiftCard> cardMap = gson.fromJson(reader, GiftCardMap.class);
            System.out.println(cardMap);

            // update giftcardMap
            giftcardMap.putAll(cardMap);
        } catch (IOException e) {
            log.warn("No /tmp/balance.json found");
        }
    }

    private void saveGiftCardMap() {
        try (Writer writer = new FileWriter("/tmp/balance.json")) {
            gson.toJson(giftcardMap, writer);
            System.out.println(gson.toJson(giftcardMap));
        } catch (IOException e) {
            log.warn("Failed to save giftcardMap to /tmp/balance.json");
        }
    }

    @Override
    public String checkBarcode(String barcode) {
        if (giftcardMap.containsKey(barcode)) {
            return checkGiftCard(barcode);
        }

        Double price = barcodePriceMap.get(barcode);
        if (started && !paymentStarted) addToCart(barcode);

        return String.format("%s:%s", barcode, price != null ? price.toString() : "");
    }

    private String checkGiftCard(String barcode) {
        GiftCard card = giftcardMap.get(barcode);
        if (paymentStarted && !transactionCompleted) { // send payment info
            paymentInfo.setCardNo(barcode);
            paymentInfo.setCardBalance(card.getBalance());
            paymentInfo.setTotalCost(currentItemMap.get("Total").getAmount());
            paymentInfo.setPinVerified(false);
            paymentInfo.setPinProtected(!StringUtils.isEmpty(card.getPin()));
            this.sendMessage("", createBarcodeMessage());
        }

        return String.format("Card No: %s, Remaining Balance: %s", barcode, card.getBalance().toString());
    }

    @Override
    public BarcodeMessage processBarcodeMessage(String sessionID, BarcodeMessage message) {

        if (message.isStarted()) {  // 0. start service
            startService();
        } else if (message.isPaymentStarted()) {
            startPayment();
        } else if (message.isCancelPayment()) {
            cancelPayment();
        } else if (message.isConfirmPayment()) {
            confirmPayment();
        } else if (message.isStartOver()) {
            startOver();
        } else if (message.isClearAll()) {   // 1. clear all
            clearAll();
        } else if (message.isDeleteFlag()) {    // 2. remove item
            removeItem(message.getBarcodeItem());
        } else { // 3. update item quantity
            updateItem(message.getBarcodeItem());
        }

        recalculateTotal();
        BarcodeMessage resp = createBarcodeMessage();
        resp.setResult("OK");

        return resp;
    }

    @Override
    public String verifyPin(String pin) {
        // check if pin matches the pin of current payment card
        if (StringUtils.isEmpty(paymentInfo.getCardNo())) {
            return "Error:No card in paymentInfo";
        }

        GiftCard card = giftcardMap.get(paymentInfo.getCardNo());

        log.info("Checking PIN for card: {}", card.getCardNo());
        if (StringUtils.isEmpty(card.getPin())) {
            return "Error: PIN is not required for this card: " + card.getCardNo();
        }

        if (!pin.equals(card.getPin())) {
            return "Error:PIN is not correct, please try again";
        }

        // update payment info
        paymentInfo.setPinVerified(true);
        this.sendMessage("", createBarcodeMessage());

        return "OK:" + card.getCardNo();
    }

    private void startOver() {
        // reset everything
        this.stopService();
        this.paymentInfo = new BarcodeMessage.PaymentInfo();
        this.paymentStarted = false;
        this.started = false;
        this.transactionCompleted = false;
        currentItemMap.clear();
        log.info("Started new transaction");
    }

    private void confirmPayment() {
        // deduct card balance
        GiftCard card = giftcardMap.get(paymentInfo.getCardNo());
        if (card == null) {
            log.error("Payment info is null?");
            return;
        }

        if (paymentInfo.isPinProtected() && !paymentInfo.isPinVerified()) {
            log.error("Payment rejected: PIN is not verified?");
            return;
        }

        if (card.getBalance() < paymentInfo.getTotalCost()) {
            log.error("Not enough balance!");
            return;
        }

        // update balance
        Double newBalance = Utils.formatDouble(card.getBalance() - paymentInfo.getTotalCost());
        card.setBalance(newBalance);
        giftcardMap.put(paymentInfo.getCardNo(), card);
        paymentInfo.setNewBalance(newBalance);

        saveGiftCardMap();

        transactionCompleted = true;
        log.info("Payment confirmed");
    }

    private void cancelPayment() {
        paymentStarted = false;

        // clear payment info
        paymentInfo = new BarcodeMessage.PaymentInfo();
        log.info("Canceled payment");
    }

    private void startPayment() {
        paymentStarted = true;
        log.info("Started payment");
    }

    @Override
    public void clearAll() {
        currentItemMap.clear();
        log.warn("Cleared all items.");
    }

    @Override
    public void removeItem(BarcodeMessage.BarcodeItem item) {
        if (item == null) return;

        currentItemMap.remove(item.getBarcode());
        log.warn("Item removed: {}", item.getBarcode());
    }

    @Override
    public void updateItem(BarcodeMessage.BarcodeItem item) {
        if (item == null) return;

        // update quantity only
        BarcodeMessage.BarcodeItem existingItem = currentItemMap.get(item.getBarcode());
        if (existingItem == null) {
            log.warn("Item not found: {}", item.getBarcode());
            return;
        }

        existingItem.updateQuantity(item.getQuantity());
    }

    @Override
    public void leave(String sessionId) {
        //stopService();
    }

    @Override
    public void startService() {
        // start timer
        timer = new Timer();
        timer.scheduleAtFixedRate(new BarCodeServiceImpl.MyTimerTask(this), 1000, 1000);

        started = true;
        log.info("started barcode service");
    }

    @Override
    public void stopService() {
        // stop timer
        if (timer != null) timer.cancel();

        started = false;
        dataChanged.set(true);
        log.info("stopped barcode service");
    }

    private void addToCart(String barcode) {
        Double price = barcodePriceMap.get(barcode);
        if (price == null) {
            log.warn("Unknown barcode: {}", barcode);
            return;
        }

        BarcodeMessage.BarcodeItem item = currentItemMap.get(barcode);
        if (item == null) {
            item = new BarcodeMessage.BarcodeItem(barcode);
            currentItemMap.put(barcode, item);
        }

        item.add(price);
        dataChanged.set(true);
    }

    public void sendMessage(String sessionID, BarcodeMessage message) {
        message.setSessionID(sessionID);
        simpMessagingTemplate.convertAndSend("/topic/barcodeStatus", message);
    }

    private void recalculateTotal() {
        final String TAG_TAX = "Tax";
        final String TAG_TOTAL = "Total";
        final double TAX_RATE = 0.13;

        currentItemMap.remove(TAG_TAX);
        currentItemMap.remove(TAG_TOTAL);

        if (currentItemMap.size() > 0) {
            double subTotal = currentItemMap.values().stream().map(BarcodeMessage.BarcodeItem::getAmount).mapToDouble(Double::doubleValue).sum();
            double taxAmt = subTotal * TAX_RATE;
            double totalAmt = subTotal + taxAmt;

            BarcodeMessage.BarcodeItem taxItem = new BarcodeMessage.BarcodeItem(TAG_TAX, 0, TAX_RATE, Utils.formatDouble(taxAmt), true);
            BarcodeMessage.BarcodeItem totalItem = new BarcodeMessage.BarcodeItem(TAG_TOTAL, 0, 0, Utils.formatDouble(totalAmt ), true);
            currentItemMap.put(TAG_TAX, taxItem);
            currentItemMap.put(TAG_TOTAL, totalItem);
        }
    }

    private BarcodeMessage createBarcodeMessage() {
        BarcodeMessage message = new BarcodeMessage();
        message.setAllItems(new ArrayList(currentItemMap.values()));
        message.setStarted(started);
        message.setPaymentStarted(paymentStarted);
        message.setPaymentInfo(paymentInfo);
        message.setTransactionCompleted(transactionCompleted);
        return message;
    }

    private class MyTimerTask extends TimerTask {
        BarCodeServiceImpl barcodeService = null;

        public MyTimerTask(BarCodeServiceImpl barcodeService) {
            this.barcodeService = barcodeService;
        }

        @Override
        public void run() {
            // send update message every seconds
            if (barcodeService.dataChanged.get()) {
                recalculateTotal();
                sendMessage("", createBarcodeMessage());

                // clear data change flag
                barcodeService.dataChanged.set(false);
            }
        }
    }

    /*
     * For Gift card management
     */
    @Override
    public List<GiftCard> getAllCards() {
        Map<String, GiftCard> cardMap = gson.fromJson(gson.toJson(giftcardMap), GiftCardMap.class);
        cardMap.values().forEach(e -> e.setPin(e.getPin().replaceAll(".", "*")));
        return new ArrayList<>(cardMap.values());
    }

    @Override
    public String changePIN(String cardNo, String oldPin, String newPin) {
        GiftCard card = giftcardMap.get(cardNo);
        if (card == null) {
            return "Error:Unknown card";
        }

        if (!oldPin.equals(card.getPin())) {
            return "Error:Current PIN is not correct";
        }

        card.setPin(newPin);
        this.saveGiftCardMap();

        log.info("Changed PIN for card: {}", card.getCardNo());

        return "OK:PIN for " + card.getCardNo() + " has been changed successfully!";
    }

    @Override
    public String transferBalance(String cardNo1, String pin, String cardNo2, double amount) {
        GiftCard card1 = giftcardMap.get(cardNo1);
        GiftCard card2 = giftcardMap.get(cardNo2);
        if (card1 == null || card2 == null) {
            return "Error:Unknown card";
        }

        if (!pin.equals(card1.getPin())) {
            return "Error:PIN is not correct";
        }

        if (amount <= 0) {
            return "Error:Amount must be > 0";
        }

        if (card1.getBalance() < amount) {
            return "Error:Not enough balance!";
        }

        card1.setBalance(Utils.formatDouble(card1.getBalance() - amount));
        card2.setBalance(Utils.formatDouble(card2.getBalance() + amount));
        this.saveGiftCardMap();

        log.info("Transferred {} from card {} to card {}", amount, cardNo1, cardNo2);

        return "OK:Transfer completed successfully!";    }
}