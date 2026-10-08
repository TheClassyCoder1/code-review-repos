package com.example.lending.loan.auditexport;

import java.io.FileOutputStream;
import java.util.Base64;

public final class ExportKeyFiles {

    private ExportKeyFiles() {
    }

    public static byte[] decoderBase64(String base64Code) {
        return Base64.getMimeDecoder().decode(base64Code);
    }

    public static void decodeBase64ToFile(String base64Code, String targetPath) throws Exception {
        byte[] buffer = decoderBase64(base64Code);
        FileOutputStream out = new FileOutputStream(targetPath);
        out.write(buffer);
        out.close();

    }
}
