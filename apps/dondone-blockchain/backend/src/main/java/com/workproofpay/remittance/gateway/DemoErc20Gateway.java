package com.workproofpay.remittance.gateway;

import com.workproofpay.remittance.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "app.chain.mode", havingValue = "demo", matchIfMissing = true)
public class DemoErc20Gateway implements Erc20Gateway {
    private final SecureRandom random = new SecureRandom();
    private final Map<String, TxState> txMap = new ConcurrentHashMap<>();
    private final AppProperties appProperties;

    public DemoErc20Gateway(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public String submitTransfer(String tokenAddress, String toAddress, long amountAtomic, String senderPrivateKey) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder("0x");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        String txHash = sb.toString();

        boolean willFail = amountAtomic % 13 == 0;
        txMap.put(txHash, new TxState(Instant.now(), willFail));
        return txHash;
    }

    @Override
    public Optional<TxReceiptResult> getReceipt(String txHash) {
        TxState state = txMap.get(txHash);
        if (state == null) {
            return Optional.empty();
        }
        long delaySeconds = appProperties.getWorker().getReceiptConfirmDelaySeconds();
        if (Instant.now().isBefore(state.createdAt.plusSeconds(delaySeconds))) {
            return Optional.empty();
        }
        if (state.willFail) {
            return Optional.of(new TxReceiptResult(false, "NETWORK_ERROR"));
        }
        return Optional.of(new TxReceiptResult(true, null));
    }

    private record TxState(Instant createdAt, boolean willFail) {}
}
