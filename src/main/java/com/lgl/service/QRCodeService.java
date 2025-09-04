package com.lgl.service;

public interface QRCodeService {
	byte[] generate(String text, int width, int height);
}
