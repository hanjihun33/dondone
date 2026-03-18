| 테스트             | 검증 내용                 |
| --------------- | --------------------- |
| recipient allow | 허용되지 않은 주소 차단         |
| cooldown        | 쿨다운 정책                |
| 정상 송금           | request → confirm     |
| idempotency     | 중복 요청 방지              |
| high amount     | 큰 금액 추가 확인            |
| operator 권한     | operator만 상태 변경       |
| 상태 전이           | invalid transition 방지 |

---
forge coverage가 낮은데 높이는 방법 연구 필요
yigd@DESKTOP-6OO0P6F:~/crypto-pjt$ forge coverage
Warning: optimizer settings and `viaIR` have been disabled for accurate coverage reports.
If you encounter "stack too deep" errors, consider using `--ir-minimum` which enables `viaIR` with minimum optimization resolving most of the errors.
See more: https://book.getfoundry.sh/guides/best-practices/stack-too-deep
[⠊] Compiling...
[⠒] Compiling 24 files with Solc 0.8.24
[⠑] Solc 0.8.24 finished in 577.41ms
Compiler run successful!
Analysing contracts...
Running tests...

Ran 7 tests for test/SafePayRemittance.t.sol:SafePayRemittanceTest
[PASS] test_HighAmountNeedsExplicitConfirmation() (gas: 225243)
[PASS] test_IdempotencyReturnsExistingRequestId() (gas: 218374)
[PASS] test_InvalidStatusTransitionReverts() (gas: 252201)
[PASS] test_OnlyOperatorCanUpdateStatus() (gas: 218393)
[PASS] test_RequestAndConfirmTransfer() (gas: 305568)
[PASS] test_RequestBlockedDuringCooldown() (gas: 47704)
[PASS] test_RequestBlockedIfRecipientNotAllowed() (gas: 22058)
Suite result: ok. 7 passed; 0 failed; 0 skipped; finished in 4.50ms (8.93ms CPU time)

Ran 1 test suite in 7.20ms (4.50ms CPU time): 7 tests passed, 0 failed, 0 skipped (7 total tests)

╭----------------------------+------------------+------------------+---------------+----------------╮
| File                       | % Lines          | % Statements     | % Branches    | % Funcs        |
+===================================================================================================+
| script/DeploySafePay.s.sol | 0.00% (0/13)     | 0.00% (0/18)     | 100.00% (0/0) | 0.00% (0/1)    |
|----------------------------+------------------+------------------+---------------+----------------|
| src/DemoStableToken.sol    | 76.92% (30/39)   | 64.86% (24/37)   | 0.00% (0/7)   | 75.00% (6/8)   |
|----------------------------+------------------+------------------+---------------+----------------|
| src/SafePayRemittance.sol  | 86.17% (81/94)   | 82.18% (83/101)  | 35.29% (6/17) | 71.43% (10/14) |
|----------------------------+------------------+------------------+---------------+----------------|
| Total                      | 76.03% (111/146) | 68.59% (107/156) | 25.00% (6/24) | 69.57% (16/23) |
╰----------------------------+------------------+------------------+---------------+----------------╯