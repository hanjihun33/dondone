// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import { Test } from "forge-std/Test.sol";
import { DemoStableToken } from "../src/DemoStableToken.sol";
import { SafePayRemittance } from "../src/SafePayRemittance.sol";

contract SafePayRemittanceTest is Test {
    DemoStableToken internal token;
    SafePayRemittance internal safePay;

    address internal alice = makeAddr("alice");
    address internal bob = makeAddr("bob");
    address internal operator = makeAddr("operator");

    uint256 internal constant THRESHOLD = 300e6; // 300 USDC

    function setUp() public {
        token = new DemoStableToken("Demo USDC", "dUSDC", 6);
        safePay = new SafePayRemittance(address(token), THRESHOLD);

        safePay.setOperator(operator, true);

        token.mint(alice, 2000e6);

        vm.prank(alice);
        token.approve(address(safePay), type(uint256).max);
    }

    function test_RequestBlockedIfRecipientNotAllowed() public {
        vm.prank(alice);
        vm.expectRevert(
            abi.encodeWithSelector(
                SafePayRemittance.PolicyBlocked.selector, "RECIPIENT_NOT_ALLOWED"
            )
        );
        safePay.requestTransfer(bob, 10e6, "k1", false);
    }

    function test_RequestBlockedDuringCooldown() public {
        vm.prank(alice);
        safePay.setRecipientAllow(bob, true);

        vm.prank(alice);
        vm.expectRevert(
            abi.encodeWithSelector(SafePayRemittance.PolicyBlocked.selector, "COOLDOWN_ACTIVE")
        );
        safePay.requestTransfer(bob, 10e6, "k1", false);
    }

    function test_RequestAndConfirmTransfer() public {
        vm.startPrank(alice);
        safePay.setRecipientAllow(bob, true);
        vm.warp(block.timestamp + safePay.COOLDOWN_SECONDS());
        uint256 requestId = safePay.requestTransfer(bob, 100e6, "idem-1", false);
        vm.stopPrank();

        vm.prank(operator);
        safePay.markSubmitted(requestId, bytes32("submitted_tx"));

        vm.prank(operator);
        safePay.markConfirmed(requestId, bytes32("confirmed_tx"));

        SafePayRemittance.TransferRequest memory req = safePay.getTransferRequest(requestId);
        assertEq(uint256(req.status), uint256(SafePayRemittance.TransferStatus.CONFIRMED));
        assertEq(token.balanceOf(alice), 1900e6);
        assertEq(token.balanceOf(bob), 100e6);
    }

    function test_IdempotencyReturnsExistingRequestId() public {
        vm.startPrank(alice);
        safePay.setRecipientAllow(bob, true);
        vm.warp(block.timestamp + safePay.COOLDOWN_SECONDS());

        uint256 first = safePay.requestTransfer(bob, 10e6, "dup-key", false);
        uint256 second = safePay.requestTransfer(bob, 10e6, "dup-key", false);
        vm.stopPrank();

        assertEq(first, second);
        assertEq(safePay.nextRequestId(), 2);
    }

    function test_HighAmountNeedsExplicitConfirmation() public {
        vm.startPrank(alice);
        safePay.setRecipientAllow(bob, true);
        vm.warp(block.timestamp + safePay.COOLDOWN_SECONDS());

        vm.expectRevert(
            abi.encodeWithSelector(
                SafePayRemittance.PolicyBlocked.selector, "HIGH_AMOUNT_CONFIRM_REQUIRED"
            )
        );
        safePay.requestTransfer(bob, THRESHOLD + 1, "high-1", false);

        uint256 requestId = safePay.requestTransfer(bob, THRESHOLD + 1, "high-2", true);
        assertEq(requestId, 1);
        vm.stopPrank();
    }

    function test_OnlyOperatorCanUpdateStatus() public {
        vm.startPrank(alice);
        safePay.setRecipientAllow(bob, true);
        vm.warp(block.timestamp + safePay.COOLDOWN_SECONDS());
        uint256 requestId = safePay.requestTransfer(bob, 10e6, "op-check", false);
        vm.stopPrank();

        vm.prank(alice);
        vm.expectRevert(SafePayRemittance.NotOperator.selector);
        safePay.markSubmitted(requestId, bytes32("tx1"));
    }

    function test_InvalidStatusTransitionReverts() public {
        vm.startPrank(alice);
        safePay.setRecipientAllow(bob, true);
        vm.warp(block.timestamp + safePay.COOLDOWN_SECONDS());
        uint256 requestId = safePay.requestTransfer(bob, 10e6, "fail-case", false);
        vm.stopPrank();

        vm.prank(operator);
        safePay.markSubmitted(requestId, bytes32("tx1"));

        vm.prank(operator);
        safePay.markFailed(requestId, "NETWORK_ERROR");

        vm.prank(operator);
        vm.expectRevert(
            abi.encodeWithSelector(
                SafePayRemittance.InvalidStatusTransition.selector,
                SafePayRemittance.TransferStatus.FAILED,
                SafePayRemittance.TransferStatus.CONFIRMED
            )
        );
        safePay.markConfirmed(requestId, bytes32("tx2"));
    }

    function test_MarkConfirmedWithoutSubmittedReverts() public {
        vm.startPrank(alice);
        safePay.setRecipientAllow(bob, true);
        vm.warp(block.timestamp + safePay.COOLDOWN_SECONDS());
        uint256 requestId = safePay.requestTransfer(bob, 10e6, "confirm-without-submit", false);
        vm.stopPrank();

        vm.prank(operator);
        vm.expectRevert(
            abi.encodeWithSelector(
                SafePayRemittance.InvalidStatusTransition.selector,
                SafePayRemittance.TransferStatus.REQUESTED,
                SafePayRemittance.TransferStatus.CONFIRMED
            )
        );
        safePay.markConfirmed(requestId, bytes32("tx2"));
    }

    function test_RequestTransferRejectsEmptyIdempotencyKey() public {
        vm.startPrank(alice);
        safePay.setRecipientAllow(bob, true);
        vm.warp(block.timestamp + safePay.COOLDOWN_SECONDS());

        vm.expectRevert(SafePayRemittance.EmptyIdempotencyKey.selector);
        safePay.requestTransfer(bob, 10e6, "", false);
        vm.stopPrank();
    }

    function test_MarkSubmittedRejectsZeroTxHash() public {
        vm.startPrank(alice);
        safePay.setRecipientAllow(bob, true);
        vm.warp(block.timestamp + safePay.COOLDOWN_SECONDS());
        uint256 requestId = safePay.requestTransfer(bob, 10e6, "zero-hash", false);
        vm.stopPrank();

        vm.prank(operator);
        vm.expectRevert(SafePayRemittance.InvalidTxHash.selector);
        safePay.markSubmitted(requestId, bytes32(0));
    }

    function test_GetTransferReceiptHashIsStable() public {
        vm.startPrank(alice);
        safePay.setRecipientAllow(bob, true);
        vm.warp(block.timestamp + safePay.COOLDOWN_SECONDS());
        uint256 requestId = safePay.requestTransfer(bob, 10e6, "receipt-hash", false);
        vm.stopPrank();

        vm.prank(operator);
        safePay.markSubmitted(requestId, bytes32("tx1"));
        vm.prank(operator);
        safePay.markConfirmed(requestId, bytes32("tx2"));

        bytes32 hash1 = safePay.getTransferReceiptHash(requestId);
        bytes32 hash2 = safePay.getTransferReceiptHash(requestId);
        assertEq(hash1, hash2);
        assertTrue(hash1 != bytes32(0));
    }
}
