package com.lgl;

import com.google.gson.Gson;
import com.lgl.service.impl.BarCodeServiceImpl;

import java.io.*;
import java.util.Map;

import static com.lgl.service.impl.BarCodeServiceImpl.*;

public class test1 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("abc".replaceAll(".", "_"));
		System.out.println("a c".replaceAll("[^ ]", "_"));
		System.out.println("a-c d".replaceAll("[^ -]", "_"));

		Gson gson = new Gson();
		try (Writer writer = new FileWriter("/tmp/balance.json")) {
			gson.toJson(giftcardMap, writer);
			System.out.println(gson.toJson(giftcardMap));
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (Reader reader = new FileReader("/tmp/balance.json")) {
			Map<String, GiftCard> giftCardMap = gson.fromJson(reader, GiftCardMap.class);
			GiftCard card = giftCardMap.get("Jack001");
			System.out.println(card);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
