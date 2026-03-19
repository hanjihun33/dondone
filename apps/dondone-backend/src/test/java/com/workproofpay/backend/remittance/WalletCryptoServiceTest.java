package com.workproofpay.backend.remittance;

import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.service.WalletCryptoService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalletCryptoServiceTest {

    @Test
    void encryptAndDecryptRoundTrip() {
        WalletCryptoService service = new WalletCryptoService(propertiesWithKey(
                "H7hbIsxfAsVlM0aMTXiG0Pv8veDSOBmTB2Hd3cCJuM4="
        ));

        String encrypted = service.encrypt("0xdeadbeef");

        assertNotEquals("0xdeadbeef", encrypted);
        assertEquals("0xdeadbeef", service.decrypt(encrypted));
    }

    @Test
    void rejectsMissingEncryptionKey() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                new WalletCryptoService(propertiesWithKey(""))
        );

        assertEquals("REMITTANCE_WALLET_ENCRYPTION_KEY is required", exception.getMessage());
    }

    @Test
    void rejectsInvalidBase64Key() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                new WalletCryptoService(propertiesWithKey("not-base64"))
        );

        assertEquals("REMITTANCE_WALLET_ENCRYPTION_KEY must be valid base64", exception.getMessage());
    }

    @Test
    void rejectsUnsupportedKeyLength() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                new WalletCryptoService(propertiesWithKey("YWFh"))
        );

        assertEquals("REMITTANCE_WALLET_ENCRYPTION_KEY must decode to 16, 24, or 32 bytes", exception.getMessage());
    }

    @Test
    void rejectsMalformedEncryptedPayload() {
        WalletCryptoService service = new WalletCryptoService(propertiesWithKey(
                "H7hbIsxfAsVlM0aMTXiG0Pv8veDSOBmTB2Hd3cCJuM4="
        ));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.decrypt("abcd")
        );

        assertEquals("wallet encrypted payload is too short", exception.getMessage());
    }

    private RemittanceProperties propertiesWithKey(String encryptionKey) {
        RemittanceProperties properties = new RemittanceProperties();
        properties.getWallet().setEncryptionKey(encryptionKey);
        return properties;
    }
}
