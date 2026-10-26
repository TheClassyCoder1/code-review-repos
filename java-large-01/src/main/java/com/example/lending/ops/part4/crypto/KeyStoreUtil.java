package com.example.lending.ops.part4.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;

/** Loads the keystores holding the partner-bank mTLS client certificates. */
public final class KeyStoreUtil {

    public enum KeyStoreType { JKS, PKCS12 }

    public static class CryptoException extends Exception {
        public CryptoException(String message, Throwable cause) { super(message, cause); }
    }

    private KeyStoreUtil() { }

    private static KeyStore getKeyStoreImpl(KeyStoreType keyStoreType) throws KeyStoreException {
        return KeyStore.getInstance(keyStoreType.name());
    }

    public static KeyStore loadKeyStore(
            File fKeyStore, char[] cKeyStorePassword, KeyStoreType keyStoreType)
            throws CryptoException, FileNotFoundException {
        KeyStore keyStore = null;
        try {
            keyStore = getKeyStoreImpl(keyStoreType);
        } catch (KeyStoreException ex) {
            throw new CryptoException("Could not create " + keyStoreType + " keystore.", ex);
        }

        FileInputStream fis = new FileInputStream(fKeyStore);
        try {
            keyStore.load(fis, cKeyStorePassword);
        } catch (GeneralSecurityException ex) {
            throw new CryptoException("Could not load keystore as type ''" + keyStoreType + "''.", ex);
        } catch (FileNotFoundException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new CryptoException("Could not load keystore as type ''" + keyStoreType + "''.", ex);
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                // Ignore
                ex.printStackTrace();
            }
        }

        return keyStore;
    }
}
