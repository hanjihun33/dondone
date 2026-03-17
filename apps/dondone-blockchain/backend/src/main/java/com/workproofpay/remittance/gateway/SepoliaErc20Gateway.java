package com.workproofpay.remittance.gateway;

import com.workproofpay.remittance.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "app.chain.mode", havingValue = "sepolia")
public class SepoliaErc20Gateway implements Erc20Gateway {
    private final AppProperties appProperties;
    private final Web3j web3j;

    public SepoliaErc20Gateway(AppProperties appProperties) {
        this.appProperties = appProperties;
        String rpcUrl = require(appProperties.getChain().getRpcUrl(), "app.chain.rpc-url");
        this.web3j = Web3j.build(new HttpService(rpcUrl));
    }

    @Override
    public String submitTransfer(String tokenAddressIgnored, String toAddress, long amountAtomic, String senderPrivateKey) {
        String tokenAddress = require(appProperties.getChain().getTokenAddress(), "app.chain.token-address");
        String privateKey = require(senderPrivateKey, "senderPrivateKey");
        BigInteger gasLimit = BigInteger.valueOf(appProperties.getChain().getGasLimit());
        BigInteger gasPrice = getGasPrice();
        Credentials credentials = Credentials.create(privateKey);
        RawTransactionManager txManager = new RawTransactionManager(
                web3j,
                credentials,
                appProperties.getChain().getChainId()
        );

        Function transfer = new Function(
                "transfer",
                java.util.List.of(new Address(toAddress), new Uint256(BigInteger.valueOf(amountAtomic))),
                Collections.emptyList()
        );

        try {
            EthSendTransaction response = txManager.sendTransaction(
                    gasPrice,
                    gasLimit,
                    tokenAddress,
                    FunctionEncoder.encode(transfer),
                    BigInteger.ZERO
            );
            if (response.hasError()) {
                throw new IllegalStateException("chain send error: " + response.getError().getMessage());
            }
            return response.getTransactionHash();
        } catch (IOException e) {
            throw new IllegalStateException("chain send failed", e);
        }
    }

    @Override
    public Optional<TxReceiptResult> getReceipt(String txHash) {
        try {
            EthGetTransactionReceipt receiptResponse = web3j.ethGetTransactionReceipt(txHash).send();
            if (receiptResponse.getTransactionReceipt().isEmpty()) {
                return Optional.empty();
            }

            var receipt = receiptResponse.getResult();
            String status = receipt.getStatus();
            if ("0x1".equalsIgnoreCase(status)) {
                return Optional.of(new TxReceiptResult(true, null));
            }
            return Optional.of(new TxReceiptResult(false, "CHAIN_REVERT"));
        } catch (IOException e) {
            return Optional.of(new TxReceiptResult(false, "NETWORK_ERROR"));
        }
    }

    private BigInteger getGasPrice() {
        try {
            EthGasPrice gasPrice = web3j.ethGasPrice().send();
            return gasPrice.getGasPrice();
        } catch (IOException e) {
            throw new IllegalStateException("failed to fetch gas price", e);
        }
    }

    private String require(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + " is required for sepolia mode");
        }
        return value;
    }
}
