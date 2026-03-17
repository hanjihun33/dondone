// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import { Test } from "forge-std/Test.sol";
import { DocumentHashRegistry } from "../src/DocumentHashRegistry.sol";

contract DocumentHashRegistryTest is Test {
    DocumentHashRegistry internal registry;

    address internal owner;
    address internal operator = makeAddr("operator");
    address internal other = makeAddr("other");

    function setUp() public {
        registry = new DocumentHashRegistry();
        owner = address(this);
    }

    function test_RegisterAndVerifyProof() public {
        registry.setOperator(operator, true);

        bytes32 proofId = keccak256("proof-pack-001");
        bytes32 payloadHash = keccak256("payload-001");

        vm.prank(operator);
        registry.registerProof(proofId, payloadHash, "PROOF_PACK", "pp_20260309_01", 4);

        assertTrue(registry.verifyProof(proofId, payloadHash));
        assertFalse(registry.verifyProof(proofId, keccak256("wrong")));

        DocumentHashRegistry.DocumentProof memory proof = registry.getProof(proofId);
        assertEq(proof.payloadHash, payloadHash);
        assertEq(proof.itemCount, 4);
        assertEq(proof.issuer, operator);
        assertEq(proof.docType, "PROOF_PACK");
    }

    function test_OnlyOperatorCanRegister() public {
        bytes32 proofId = keccak256("proof-pack-002");
        bytes32 payloadHash = keccak256("payload-002");

        vm.prank(other);
        vm.expectRevert(DocumentHashRegistry.NotOperator.selector);
        registry.registerProof(proofId, payloadHash, "CLAIM_KIT", "ck_20260309_01", 2);
    }

    function test_ReRegisterSameProofReverts() public {
        bytes32 proofId = keccak256("proof-pack-003");
        bytes32 payloadHash = keccak256("payload-003");

        registry.registerProof(proofId, payloadHash, "PROOF_PACK", "pp_20260309_03", 1);

        vm.expectRevert(DocumentHashRegistry.ProofAlreadyExists.selector);
        registry.registerProof(proofId, payloadHash, "PROOF_PACK", "pp_20260309_03", 1);
    }

    function test_GetUnknownProofReverts() public {
        vm.expectRevert(DocumentHashRegistry.UnknownProof.selector);
        registry.getProof(keccak256("unknown"));
    }

    function test_VerifyUnknownProofReturnsFalse() public {
        assertFalse(registry.verifyProof(keccak256("unknown"), keccak256("payload")));
    }

    function test_RegisterRejectsInvalidInput() public {
        vm.expectRevert(DocumentHashRegistry.InvalidHash.selector);
        registry.registerProof(bytes32(0), keccak256("payload"), "PROOF_PACK", "pp_1", 1);

        vm.expectRevert(DocumentHashRegistry.EmptyDocType.selector);
        registry.registerProof(keccak256("id1"), keccak256("payload"), "", "pp_1", 1);

        vm.expectRevert(DocumentHashRegistry.EmptySourceRef.selector);
        registry.registerProof(keccak256("id1"), keccak256("payload"), "PROOF_PACK", "", 1);

        vm.expectRevert(DocumentHashRegistry.InvalidItemCount.selector);
        registry.registerProof(keccak256("id1"), keccak256("payload"), "PROOF_PACK", "pp_1", 0);
    }

    function test_SetOperatorOnlyOwner() public {
        vm.prank(other);
        vm.expectRevert(DocumentHashRegistry.NotOwner.selector);
        registry.setOperator(other, true);
    }

    function test_TransferOwnership() public {
        registry.transferOwnership(other);

        vm.expectRevert(DocumentHashRegistry.NotOwner.selector);
        registry.setOperator(operator, true);

        vm.prank(other);
        registry.setOperator(operator, true);
        assertTrue(registry.operators(operator));
    }
}
