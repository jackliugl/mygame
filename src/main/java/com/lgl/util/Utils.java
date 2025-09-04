package com.lgl.util;

public class Utils {

    public static double formatDouble(double value) {
        return Math.floor(value * 100 + 0.5) / 100;
    }
}
