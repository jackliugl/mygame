package com.lgl.service.impl;

import org.springframework.stereotype.Service;

import com.lgl.service.QRCodeService;

import net.glxn.qrgen.javase.QRCode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class QRCodeServiceImpl implements QRCodeService {

	@Override
	public byte[] generate(String text, int width, int height) {

		// QRGen: a simple QRCode generation api for java built on top ZXING
		// https://www.websparrow.org/spring/generate-qr-code-using-spring-boot-rest-api
		try (ByteArrayOutputStream bos = QRCode.from(text).withSize(width, height).stream();) {

			return bos.toByteArray();

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}