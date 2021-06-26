package com.example.bledongle;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptDecryptString {
    private static final String encryptionKey           = "bd235b09968f4cae974b1300304b6be2";
    private static final String characterEncoding       = "UTF-8";
    private static final String cipherTransformation    = "AES/CTR/NoPadding";
    private static final String aesEncryptionAlgorithem = "AES";


    /**
     * Method for Encrypt Plain String Data
     * @param plainText
     * @return encryptedText
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static byte[] encrypt(byte[] inputs) {  // input hex bytes
        String encryptedText = "";
        byte[] cipherText = {};
        try {
            Cipher cipher   = Cipher.getInstance(cipherTransformation);
            byte a = (byte) 0xbd;
            byte[] key      = {(byte)0xbd, 0x23, 0x5b, 0x09, (byte)0x96, (byte)0x8f, 0x4c, (byte)0xae, (byte)0x97, 0x4b, 0x13, 0x00, 0x30, 0x4b, 0x6b, (byte)0xe2};//encryptionKey.getBytes(characterEncoding);
//            byte[] key      = {0xb, 0xd, 0x2, 0x3, 0x5, 0xb, 0x0, 0x9, 0x9, 0x6, 0x8, 0xf, 0x4, 0xc, 0xa, 0xe, 0x9, 0x7, 0x4, 0xb, 0x1, 0x3, 0x0, 0x0, 0x3, 0x0, 0x4, 0xb, 0x6, 0xb, 0xe, 0x2};//encryptionKey.getBytes(characterEncoding);
            SecretKeySpec secretKey = new SecretKeySpec(key, aesEncryptionAlgorithem);
            byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            IvParameterSpec ivparameterspec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivparameterspec);
            cipherText = cipher.doFinal(inputs);

            encryptedText = bytesToHex(cipherText);
//            Base64.Encoder encoder = Base64.getEncoder();
//            encryptedText = encoder.encodeToString(cipherText);

        } catch (Exception E) {
            System.err.println("Encrypt Exception : "+E.getMessage());
        }
        return cipherText;
    }
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
    /**
     * Method For Get encryptedText and Decrypted provided String
     * @param encryptedText
     * @return decryptedText
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String decrypt(String encryptedText) {
        String decryptedText = "";
        try {
            Cipher cipher = Cipher.getInstance(cipherTransformation);
            byte[] key = encryptionKey.getBytes(characterEncoding);
            SecretKeySpec secretKey = new SecretKeySpec(key, aesEncryptionAlgorithem);
            IvParameterSpec ivparameterspec = new IvParameterSpec(key);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivparameterspec);
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] cipherText = decoder.decode(encryptedText.getBytes("UTF8"));
            decryptedText = new String(cipher.doFinal(cipherText), "UTF-8");

        } catch (Exception E) {
            System.err.println("decrypt Exception : "+E.getMessage());
        }
        return decryptedText;
    }

}
