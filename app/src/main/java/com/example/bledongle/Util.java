package com.example.bledongle;

import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class Util {
    public static final String baseBluetoothUuidPostfix = "0000-1000-8000-00805f9b34fb";
    public static final String bleAddress = "DF:43:94:2E:1E:03";
    public static final String dongleUUID = "1800";
    public static final String serviceUUID = "c873ac9f-f829-468e-9954-f56cdc4abac5";
    public static final String characterUUID = "c8730002-f829-468e-9954-f56cdc4abac5";
    public static final String notifyUUID = "c8730001-f829-468e-9954-f56cdc4abac5";
    public static final String switchUUID = "c8730003-f829-468e-9954-f56cdc4abac5";
    public static final int pinCode = 5;
    public static final int default_packet = 200;
    public static final int default_mtu = 240;
    public final static String INTENT_PREFIX = "USBD HID";
    public final static String ACTION_GATT_CONNECTED = INTENT_PREFIX+".ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = INTENT_PREFIX+".ACTION_GATT_DISCONNECTED";

    public static void showAlert(Context context, String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }
    public static byte[] combineBytes(byte[] a, byte[] b) {
        byte[] combined = new byte[a.length + b.length];

        for (int i = 0; i < combined.length; ++i)
        {
            combined[i] = i < a.length ? a[i] : b[i - a.length];
        }
        return combined;
    }
    public static String toHex(String arg) {
        return String.format("%x", new BigInteger(1, arg.getBytes(/*YOUR_CHARSET?*/)));
    }
    public static String asciiToHex(String asciiStr) {
        char[] chars = asciiStr.toCharArray();
        StringBuilder hex = new StringBuilder();
        for (char ch : chars) {
            hex.append(Integer.toHexString((int) ch));
        }

        return hex.toString();
    }
    public static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder("");

        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }

        return output.toString();
    }

    public static byte hexToByte(String hexString) {
        int firstDigit = toDigit(hexString.charAt(0));
        int secondDigit = toDigit(hexString.charAt(1));
        return (byte) ((firstDigit << 4) + secondDigit);
    }

    private static int toDigit(char hexChar) {
        int digit = Character.digit(hexChar, 16);
        if(digit == -1) {
            throw new IllegalArgumentException(
                    "Invalid Hexadecimal Character: "+ hexChar);
        }
        return digit;
    }
    public static byte[] decodeAsciiString(String asciiString) {
        byte[] bytes = new byte[1];
        int i = Integer.valueOf(asciiString);
        return bytes;
    }
    public static byte[] decodeHexString(String hexString) {
        if (hexString.length() % 2 == 1) {
            throw new IllegalArgumentException(
                    "Invalid hexadecimal String supplied.");
        }

        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = hexToByte(hexString.substring(i, i + 2));
        }
        return bytes;
    }


    public static  byte[] my_int_to_bb_le(int myInteger){
        return ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN).putInt(myInteger).array();
    }

    public static int my_bb_to_int_le(byte [] byteBarray){
        return ByteBuffer.wrap(byteBarray).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static  byte[] IntToPasskey_be(int myInteger){
        return ByteBuffer.allocate(6).order(ByteOrder.BIG_ENDIAN).putInt(myInteger).array();
    }

    public static int my_bb_to_int_be(byte [] byteBarray){
        return ByteBuffer.wrap(byteBarray).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    public static UUID uuidFromShortCode16(String shortCode16) {
        return UUID.fromString("0000" + shortCode16 + "-" + baseBluetoothUuidPostfix);
    }

    public static UUID uuidFromShortCode32(String shortCode32) {
        return UUID.fromString(shortCode32 + "-" + baseBluetoothUuidPostfix);
    }

    public static UUID uuidFromString(String code) {
        return UUID.fromString(code);
    }
}
