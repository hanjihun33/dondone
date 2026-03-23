package com.workproofpay.backend.remittance.service;

import com.workproofpay.backend.jobs.model.JobType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RemittanceMetrics {

    private static final String UNKNOWN = "unknown";

    private final MeterRegistry meterRegistry;

    public Timer.Sample start() {
        return Timer.start(meterRegistry);
    }

    public void recordTransferCreate(Timer.Sample sample, String outcome, boolean replayed) {
        stop(sample, "dondone.remittance.transfer.create",
                "outcome", sanitize(outcome),
                "replayed", Boolean.toString(replayed)
        );
    }

    public void recordPolicyEvaluate(Timer.Sample sample, String outcome, String policyCode) {
        stop(sample, "dondone.remittance.policy.evaluate",
                "outcome", sanitize(outcome),
                "policy_code", sanitize(policyCode)
        );
    }

    public void recordWalletLookup(Timer.Sample sample, String lookupMode, String outcome) {
        stop(sample, "dondone.remittance.wallet.lookup",
                "lookup_mode", sanitize(lookupMode),
                "outcome", sanitize(outcome)
        );
    }

    public void recordWorkerJob(Timer.Sample sample, JobType jobType, String outcome) {
        stop(sample, "dondone.remittance.worker.job",
                "job_type", jobType == null ? UNKNOWN : jobType.name().toLowerCase(Locale.ROOT),
                "outcome", sanitize(outcome)
        );
    }

    public void recordChainOperation(Timer.Sample sample, String mode, String operation, String outcome) {
        stop(sample, "dondone.remittance.chain.operation",
                "mode", sanitize(mode),
                "operation", sanitize(operation),
                "outcome", sanitize(outcome)
        );
    }

    public void recordJobQueueDelay(Duration duration, JobType jobType) {
        Timer.builder("dondone.remittance.job.queue.delay")
                .tags("job_type", jobType == null ? UNKNOWN : jobType.name().toLowerCase(Locale.ROOT))
                .register(meterRegistry)
                .record(toNanos(duration), TimeUnit.NANOSECONDS);
    }

    public void recordTransferLifecycle(Duration duration, String terminalStatus) {
        Timer.builder("dondone.remittance.transfer.lifecycle")
                .tags("terminal_status", sanitize(terminalStatus))
                .register(meterRegistry)
                .record(toNanos(duration), TimeUnit.NANOSECONDS);
    }

    public void recordBroadcastToTerminal(Duration duration, String terminalStatus) {
        Timer.builder("dondone.remittance.transfer.broadcast_to_terminal")
                .tags("terminal_status", sanitize(terminalStatus))
                .register(meterRegistry)
                .record(toNanos(duration), TimeUnit.NANOSECONDS);
    }

    public void recordRequestToBroadcast(Duration duration) {
        Timer.builder("dondone.remittance.transfer.request_to_broadcast")
                .register(meterRegistry)
                .record(toNanos(duration), TimeUnit.NANOSECONDS);
    }

    private void stop(Timer.Sample sample, String metricName, String... tags) {
        sample.stop(Timer.builder(metricName).tags(tags).register(meterRegistry));
    }

    private long toNanos(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return 0L;
        }
        return duration.toNanos();
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return value;
    }
}
