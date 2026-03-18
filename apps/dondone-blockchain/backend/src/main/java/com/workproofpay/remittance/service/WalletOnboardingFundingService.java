package com.workproofpay.remittance.service;

import com.workproofpay.remittance.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;

@Component
public class WalletOnboardingFundingService {
    private static final Logger log = LoggerFactory.getLogger(WalletOnboardingFundingService.class);

    private final AppProperties appProperties;

    public WalletOnboardingFundingService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWalletCreated(WalletCreatedEvent event) {
        if (!"sepolia".equalsIgnoreCase(appProperties.getChain().getMode())) {
            return;
        }
        if (!appProperties.getChain().getOnboarding().isEnabled()) {
            return;
        }

        String rpcUrl = appProperties.getChain().getRpcUrl();
        String tokenAddress = appProperties.getChain().getTokenAddress();
        String fundingPrivateKey = appProperties.getChain().getOnboarding().getFundingPrivateKey();
        if (isBlank(rpcUrl) || isBlank(tokenAddress) || isBlank(fundingPrivateKey)) {
            log.warn("wallet onboarding funding skipped for userId={} due to missing chain funding config", event.userId());
            return;
        }

        Web3j web3j = Web3j.build(new HttpService(rpcUrl));
        try {
            Credentials funder = Credentials.create(fundingPrivateKey);
            if (funder.getAddress().equalsIgnoreCase(event.walletAddress())) {
                log.info("wallet onboarding funding skipped for userId={} because wallet is treasury address", event.userId());
                return;
            }

            RawTransactionManager txManager = new RawTransactionManager(
                    web3j,
                    funder,
                    appProperties.getChain().getChainId()
            );

            BigInteger gasPrice = gasPrice(web3j);
            fundEth(web3j, txManager, gasPrice, event.walletAddress());
            fundStablecoin(txManager, gasPrice, tokenAddress, event.walletAddress());
            log.info("wallet onboarding funding completed for userId={} wallet={}", event.userId(), event.walletAddress());
        } catch (Exception e) {
            log.error("wallet onboarding funding failed for userId={} wallet={}", event.userId(), event.walletAddress(), e);
        } finally {
            web3j.shutdown();
        }
    }

    private void fundEth(Web3j web3j, RawTransactionManager txManager, BigInteger gasPrice, String walletAddress) throws IOException {
        BigInteger ethAmountWei = new BigInteger(appProperties.getChain().getOnboarding().getEthAmountWei());
        if (ethAmountWei.signum() <= 0) {
            return;
        }
        EthSendTransaction response = txManager.sendTransaction(
                gasPrice,
                BigInteger.valueOf(21_000),
                walletAddress,
                "",
                ethAmountWei
        );
        if (response.hasError()) {
            throw new IllegalStateException("onboarding eth funding failed: " + response.getError().getMessage());
        }
    }

    private void fundStablecoin(RawTransactionManager txManager, BigInteger gasPrice, String tokenAddress, String walletAddress) throws IOException {
        long stablecoinAmountAtomic = appProperties.getChain().getOnboarding().getStablecoinAmountAtomic();
        if (stablecoinAmountAtomic <= 0) {
            return;
        }

        Function transfer = new Function(
                "transfer",
                java.util.List.of(new Address(walletAddress), new Uint256(BigInteger.valueOf(stablecoinAmountAtomic))),
                Collections.emptyList()
        );

        EthSendTransaction response = txManager.sendTransaction(
                gasPrice,
                BigInteger.valueOf(appProperties.getChain().getGasLimit()),
                tokenAddress,
                FunctionEncoder.encode(transfer),
                BigInteger.ZERO
        );
        if (response.hasError()) {
            throw new IllegalStateException("onboarding stablecoin funding failed: " + response.getError().getMessage());
        }
    }

    private BigInteger gasPrice(Web3j web3j) throws IOException {
        EthGasPrice gasPrice = web3j.ethGasPrice().send();
        return gasPrice.getGasPrice();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
