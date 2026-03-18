package com.workproofpay.backend.remittance.adapter;

import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.model.TransferFailureCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "remittance.chain.mode", havingValue = "sepolia", matchIfMissing = true)
public class SepoliaRemittanceBlockchainGateway implements RemittanceBlockchainGateway {

    private final RemittanceProperties properties;
    private Web3j web3j;

    public SepoliaRemittanceBlockchainGateway(RemittanceProperties properties) {
        this.properties = properties;
    }

    @Override
    public ChainBalanceSnapshot getBalances(String walletAddress) {
        try {
            EthGetBalance nativeBalanceResponse = web3j().ethGetBalance(walletAddress, DefaultBlockParameterName.LATEST).send();
            BigInteger nativeBalanceWei = nativeBalanceResponse.getBalance();
            BigInteger tokenBalanceAtomic = readTokenBalance(walletAddress);
            return new ChainBalanceSnapshot(tokenBalanceAtomic, nativeBalanceWei);
        } catch (IOException e) {
            throw new IllegalStateException("failed to fetch chain balances", e);
        }
    }

    @Override
    public void fundWallet(String walletAddress) {
        String treasuryKey = require(properties.getTreasury().getPrivateKey(), "REMITTANCE_TREASURY_PRIVATE_KEY");
        Credentials treasury = Credentials.create(treasuryKey);
        RawTransactionManager txManager = new RawTransactionManager(web3j(), treasury, properties.getChain().getChainId());
        BigInteger gasPrice = getGasPrice();

        try {
            EthSendTransaction nativeTx = txManager.sendTransaction(
                    gasPrice,
                    BigInteger.valueOf(properties.getChain().getNativeTransferGasLimit()),
                    walletAddress,
                    "",
                    new BigInteger(properties.getTreasury().getInitialNativeAmountWei())
            );
            if (nativeTx.hasError()) {
                throw new IllegalStateException("native funding failed: " + nativeTx.getError().getMessage());
            }
            waitForSuccessfulReceipt(require(nativeTx.getTransactionHash(), "nativeFundingTxHash"));

            Function transfer = new Function(
                    "transfer",
                    List.of(
                            new Address(walletAddress),
                            new Uint256(BigInteger.valueOf(properties.getTreasury().getInitialTokenAmountAtomic()))
                    ),
                    Collections.emptyList()
            );
            EthSendTransaction tokenTx = txManager.sendTransaction(
                    gasPrice,
                    BigInteger.valueOf(properties.getChain().getTokenGasLimit()),
                    require(properties.getChain().getTokenAddress(), "REMITTANCE_TOKEN_ADDRESS"),
                    FunctionEncoder.encode(transfer),
                    BigInteger.ZERO
            );
            if (tokenTx.hasError()) {
                throw new IllegalStateException("token funding failed: " + tokenTx.getError().getMessage());
            }
            waitForSuccessfulReceipt(require(tokenTx.getTransactionHash(), "tokenFundingTxHash"));
        } catch (IOException e) {
            throw new IllegalStateException("wallet funding failed", e);
        }
    }

    @Override
    public PreparedTokenTransfer prepareTokenTransfer(String senderPrivateKey, String toAddress, BigInteger amountAtomic) {
        Credentials credentials = Credentials.create(require(senderPrivateKey, "senderPrivateKey"));
        Function transfer = new Function(
                "transfer",
                List.of(new Address(toAddress), new Uint256(amountAtomic)),
                Collections.emptyList()
        );

        try {
            BigInteger nonce = getTransactionCount(credentials.getAddress());
            BigInteger gasPrice = getGasPrice();
            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    nonce,
                    gasPrice,
                    BigInteger.valueOf(properties.getChain().getTokenGasLimit()),
                    require(properties.getChain().getTokenAddress(), "REMITTANCE_TOKEN_ADDRESS"),
                    BigInteger.ZERO,
                    FunctionEncoder.encode(transfer)
            );
            String signedTransaction = Numeric.toHexString(
                    TransactionEncoder.signMessage(rawTransaction, properties.getChain().getChainId(), credentials)
            );
            String txHash = Hash.sha3(signedTransaction);
            return new PreparedTokenTransfer(txHash, signedTransaction);
        } catch (IOException e) {
            throw new IllegalStateException("token transfer preparation failed", e);
        }
    }

    @Override
    public void broadcastSignedTransaction(String signedTransaction) {
        try {
            EthSendTransaction response = web3j().ethSendRawTransaction(require(signedTransaction, "signedTransaction")).send();
            if (response.hasError() && !isAlreadyKnown(response.getError().getMessage())) {
                throw new IllegalStateException("chain send error: " + response.getError().getMessage());
            }
        } catch (IOException e) {
            throw new IllegalStateException("token transfer submission failed", e);
        }
    }

    @Override
    public Optional<ChainReceiptResult> getReceipt(String txHash) {
        try {
            EthGetTransactionReceipt receiptResponse = web3j().ethGetTransactionReceipt(txHash).send();
            if (receiptResponse.getTransactionReceipt().isEmpty()) {
                return Optional.empty();
            }
            String status = receiptResponse.getResult().getStatus();
            if ("0x1".equalsIgnoreCase(status)) {
                return Optional.of(new ChainReceiptResult(true, null));
            }
            return Optional.of(new ChainReceiptResult(false, TransferFailureCode.CHAIN_REVERT));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean isTransactionKnown(String txHash) {
        try {
            EthTransaction transactionResponse = web3j().ethGetTransactionByHash(txHash).send();
            return transactionResponse.getTransaction().isPresent();
        } catch (IOException e) {
            throw new IllegalStateException("failed to look up transaction", e);
        }
    }

    private BigInteger readTokenBalance(String walletAddress) throws IOException {
        Function function = new Function(
                "balanceOf",
                List.of(new Address(walletAddress)),
                List.of(new TypeReference<Uint256>() {
                })
        );
        String encoded = FunctionEncoder.encode(function);
        EthCall response = web3j().ethCall(
                Transaction.createEthCallTransaction(walletAddress, require(properties.getChain().getTokenAddress(), "REMITTANCE_TOKEN_ADDRESS"), encoded),
                DefaultBlockParameterName.LATEST
        ).send();
        List<Type> outputs = org.web3j.abi.FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        if (outputs.isEmpty()) {
            return BigInteger.ZERO;
        }
        return (BigInteger) outputs.get(0).getValue();
    }

    private void waitForSuccessfulReceipt(String txHash) {
        Instant deadline = Instant.now().plusSeconds(properties.getWallet().getFundingReceiptTimeoutSeconds());
        while (Instant.now().isBefore(deadline)) {
            Optional<ChainReceiptResult> receiptResult = getReceipt(txHash);
            if (receiptResult.isPresent()) {
                if (!receiptResult.get().success()) {
                    throw new IllegalStateException("funding transaction reverted: " + txHash);
                }
                return;
            }
            try {
                Thread.sleep(1_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("funding receipt wait interrupted", e);
            }
        }
        throw new IllegalStateException("funding receipt timed out: " + txHash);
    }

    private BigInteger getGasPrice() {
        try {
            EthGasPrice gasPrice = web3j().ethGasPrice().send();
            return gasPrice.getGasPrice();
        } catch (IOException e) {
            throw new IllegalStateException("failed to fetch gas price", e);
        }
    }

    private BigInteger getTransactionCount(String walletAddress) throws IOException {
        EthGetTransactionCount transactionCount = web3j()
                .ethGetTransactionCount(walletAddress, DefaultBlockParameterName.PENDING)
                .send();
        return transactionCount.getTransactionCount();
    }

    private Web3j web3j() {
        if (web3j == null) {
            web3j = Web3j.build(new HttpService(require(properties.getChain().getRpcUrl(), "REMITTANCE_CHAIN_RPC_URL")));
        }
        return web3j;
    }

    private String require(String value, String envName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(envName + " is required");
        }
        return value;
    }

    private boolean isAlreadyKnown(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("already known")
                || normalized.contains("already imported")
                || normalized.contains("known transaction");
    }
}
