package com.example.lending.loan.partner.envelope;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class EnvelopeTools {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EnvelopeTools() {
    }

    public static EnvelopeResponse valid(EnvelopeRequest req) throws GeneralSecurityException, IOException {
        EnvelopeResponse resp=new EnvelopeResponse();
        String pubKey=req.getPubKey();
        String aesKey=req.getAesKey();
        String data=req.getData();
        String signData=req.getSignData();
        PublicKey rsa=KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pubKey)));
        Signature sign=Signature.getInstance("SHA1withRSA");
        sign.initVerify(rsa);

        Cipher rsaCipher=Cipher.getInstance("RSA");
        rsaCipher.init(Cipher.DECRYPT_MODE, rsa);
        byte[] decryptAes = rsaCipher.doFinal(Base64.getDecoder().decode(aesKey));
        Cipher aes = Cipher.getInstance("AES");
        aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptAes, "AES"));

        String dencrptValue =new String(aes.doFinal(Base64.getDecoder().decode(data)), StandardCharsets.UTF_8);
        resp.setData(MAPPER.readTree(dencrptValue));

        sign.update(dencrptValue.getBytes(StandardCharsets.UTF_8));
        boolean verify = sign.verify(Base64.getDecoder().decode(signData));
        resp.setSuccess(verify);
        return resp;
    }
}
