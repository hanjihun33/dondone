package com.workproofpay.backend.remittance.adapter;

import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.model.TransferFailureCode;
import com.workproofpay.backend.remittance.service.RemittanceMetrics;
import io.micrometer.core.instrument.Timer;
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
    private final RemittanceMetrics remittanceMetrics;
    private Web3j web3j;

    public SepoliaRemittanceBlockchainGateway(RemittanceProperties properties, RemittanceMetrics remittanceMetrics) {
        this.properties = properties;
        this.remittanceMetrics = remittanceMetrics;
    }

    @Override
    public ChainBalanceSnapshot getBalances(String walletAddress) {
        Timer.Sample sample = remittanceMetrics.start();
        String outcome = "success";
        try {
            EthGetBalance nativeBalanceResponse = web3j().ethGetBalance(walletAddress, DefaultBlockParameterName.LATEST).send();
            BigInteger nativeBalanceWei = nativeBalanceResponse.getBalance();
            BigInteger tokenBalanceAtomic = readTokenBalance(walletAddress);
            return new ChainBalanceSnapshot(tokenBalanceAtomic, nativeBalanceWei);
        } catch (IOException e) {
            outcome = "error";
            throw new IllegalStateException("failed to fetch chain balances", e);
        } finally {
            remittanceMetrics.recordChainOperation(sample, properties.getChain().getMode(), "get_balances", outcome);
        }
    }

    @Override
    public void fundWallet(String walletAddress, BigInteger tokenAmountAtomic, BigInteger nativeAmountWei) {
        Timer.Sample sample = remittanceMetrics.start();
        String outcome = "success";
        String treasuryKey = require(properties.getTreasury().getPrivateKey(), "REMITTANCE_TREASURY_PRIVATE_KEY");
        Credentials treasury = Credentials.create(treasuryKey);
        RawTransactionManager txManager = new RawTransactionManager(web3j(), treasury, properties.getChain().getChainId());
        BigInteger gasPrice = getGasPrice();

        try {
            if (nativeAmountWei.signum() > 0) {
                EthSendTransaction nativeTx = txManager.sendTransaction(
                        gasPrice,
                        BigInteger.valueOf(properties.getChain().getNativeTransferGasLimit()),
                        walletAddress,
                        "",
                        nativeAmountWei
                );
                if (nativeTx.hasError()) {
                    throw new IllegalStateException("native funding failed: " + nativeTx.getError().getMessage());
                }
                waitForSuccessfulReceipt(require(nativeTx.getTransactionHash(), "nativeFundingTxHash"));
            }

            if (tokenAmountAtomic.signum() > 0) {
                Function transfer = new Function(
                        "transfer",
                        List.of(
                                new Address(walletAddress),
                                new Uint256(tokenAmountAtomic)
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
            }
        } catch (IOException e) {
            outcome = "error";
            throw new IllegalStateException("wallet funding failed", e);
        } catch (RuntimeException e) {
            outcome = "error";
            throw e;
        } finally {
            remittanceMetrics.recordChainOperation(sample, properties.getChain().getMode(), "fund_wallet", outcome);
        }
    }

    @Override
    public BigInteger estimateTokenTransferGasCostWei() {
        Timer.Sample sample = remittanceMetrics.start();
        String outcome = "success";
        try {
            return getGasPrice().multiply(BigInteger.valueOf(properties.getChain().getTokenGasLimit()));
        } catch (RuntimeException e) {
            outcome = "error";
            throw e;
        } finally {
            remittanceMetrics.recordChainOperation(sample, properties.getChain().getMode(), "estimate_transfer_gas", outcome);
        }
    }

    @Override
    public PreparedTokenTransfer prepareTokenTransfer(String senderPrivateKey, String toAddress, BigInteger amountAtomic) {
        Timer.Sample sample = remittanceMetrics.start();
        String outcome = "success";
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
            outcome = "error";
            throw new IllegalStateException("token transfer preparation failed", e);
        } catch (RuntimeException e) {
            outcome = "error";
            throw e;
        } finally {
            remittanceMetrics.recordChainOperation(sample, properties.getChain().getMode(), "prepare_transfer", outcome);
        }
    }

    @Override
    public void broadcastSignedTransaction(String signedTransaction) {
        Timer.Sample sample = remittanceMetrics.start();
        String outcome = "success";
        try {
            EthSendTransaction response = web3j().ethSendRawTransaction(require(signedTransaction, "signedTransaction")).send();
            if (response.hasError() && !isAlreadyKnown(response.getError().getMessage())) {
                outcome = "chain_error";
                throw new IllegalStateException("chain send error: " + response.getError().getMessage());
            }
        } catch (IOException e) {
            outcome = "error";
            throw new IllegalStateException("token transfer submission failed", e);
        } catch (RuntimeException e) {
            if ("success".equals(outcome)) {
                outcome = "error";
            }
            throw e;
        } finally {
            remittanceMetrics.recordChainOperation(sample, properties.getChain().getMode(), "broadcast_signed_transaction", outcome);
        }
    }

    @Override
    public Optional<ChainReceiptResult> getReceipt(String txHash) {
        Timer.Sample sample = remittanceMetrics.start();
        String outcome = "empty";
        try {
            EthGetTransactionReceipt receiptResponse = web3j().ethGetTransactionReceipt(txHash).send();
            if (receiptResponse.getTransactionReceipt().isEmpty()) {
                return Optional.empty();
            }
            String status = receiptResponse.getResult().getStatus();
            if ("0x1".equalsIgnoreCase(status)) {
                outcome = "success";
                return Optional.of(new ChainReceiptResult(true, null));
            }
            outcome = "failed";
            return Optional.of(new ChainReceiptResult(false, TransferFailureCode.CHAIN_REVERT));
        } catch (IOException e) {
            outcome = "error";
            return Optional.empty();
        } finally {
            remittanceMetrics.recordChainOperation(sample, properties.getChain().getMode(), "get_receipt", outcome);
        }
    }

    @Override
    public boolean isTransactionKnown(String txHash) {
        Timer.Sample sample = remittanceMetrics.start();
        String outcome = "success";
        try {
            EthTransaction transactionResponse = web3j().ethGetTransactionByHash(txHash).send();
            return transactionResponse.getTransaction().isPresent();
        } catch (IOException e) {
            outcome = "error";
            throw new IllegalStateException("failed to look up transaction", e);
        } finally {
            remittanceMetrics.recordChainOperation(sample, properties.getChain().getMode(), "is_transaction_known", outcome);
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
