package com.workproofpay.backend.remittance;

import com.workproofpay.backend.remittance.model.Transfer;
import com.workproofpay.backend.remittance.model.TransferFailureCode;
import com.workproofpay.backend.remittance.model.TransferStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferStateMachineTest {

    @Test
    void followsHappyPathFromRequestedToConfirmed() {
        Transfer transfer = requestedTransfer();

        transfer.markSigned("0xabc", "signed-tx");
        transfer.markBroadcasted();
        transfer.markConfirmed();

        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.CONFIRMED);
        assertThat(transfer.getSignedTransaction()).isNull();
    }

    @Test
    void rejectsInvalidStateTransition() {
        Transfer transfer = requestedTransfer();

        assertThatThrownBy(transfer::markBroadcasted)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid transfer state transition");
    }

    @Test
    void allowsRetryAfterTerminalFailure() {
        Transfer transfer = requestedTransfer();
        transfer.markSigned("0xabc", "signed-tx");
        transfer.markBroadcasted();
        transfer.markTimedOut(TransferFailureCode.NETWORK_ERROR);

        transfer.resetForRetry();

        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.REQUESTED);
        assertThat(transfer.getTxHash()).isNull();
        assertThat(transfer.getFailureCode()).isNull();
    }

    private Transfer requestedTransfer() {
        Transfer transfer = Transfer.request(
                "tr_state",
                1L,
                "rcp_state",
                "dUSDC",
                50_000_000L,
                "0x1111111111111111111111111111111111111111",
                "0x2222222222222222222222222222222222222222",
                "idem-state",
                false,
                true
        );
        transfer.onCreate();
        return transfer;
    }
}
