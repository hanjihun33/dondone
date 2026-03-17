// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

contract DocumentHashRegistry {
    struct DocumentProof {
        bytes32 payloadHash;
        uint64 createdAt;
        uint32 itemCount;
        address issuer;
        string docType;
        string sourceRef;
    }

    address public owner;
    mapping(address => bool) public operators;
    mapping(bytes32 => DocumentProof) private proofs;

    event OwnershipTransferred(address indexed previousOwner, address indexed newOwner);
    event OperatorUpdated(address indexed operator, bool allowed);
    event DocumentProofRegistered(
        bytes32 indexed proofId,
        bytes32 indexed payloadHash,
        string docType,
        string sourceRef,
        uint32 itemCount,
        address indexed issuer,
        uint256 createdAt
    );

    error NotOwner();
    error NotOperator();
    error InvalidAddress();
    error InvalidHash();
    error InvalidItemCount();
    error EmptyDocType();
    error EmptySourceRef();
    error ProofAlreadyExists();
    error UnknownProof();

    modifier onlyOwner() {
        if (msg.sender != owner) revert NotOwner();
        _;
    }

    modifier onlyOperator() {
        if (!operators[msg.sender]) revert NotOperator();
        _;
    }

    constructor() {
        owner = msg.sender;
        operators[msg.sender] = true;
        emit OwnershipTransferred(address(0), msg.sender);
        emit OperatorUpdated(msg.sender, true);
    }

    function transferOwnership(
        address newOwner
    ) external onlyOwner {
        if (newOwner == address(0)) revert InvalidAddress();
        emit OwnershipTransferred(owner, newOwner);
        owner = newOwner;
    }

    function setOperator(
        address operator,
        bool allowed
    ) external onlyOwner {
        if (operator == address(0)) revert InvalidAddress();
        operators[operator] = allowed;
        emit OperatorUpdated(operator, allowed);
    }

    function registerProof(
        bytes32 proofId,
        bytes32 payloadHash,
        string calldata docType,
        string calldata sourceRef,
        uint32 itemCount
    ) external onlyOperator {
        if (proofId == bytes32(0) || payloadHash == bytes32(0)) revert InvalidHash();
        if (bytes(docType).length == 0) revert EmptyDocType();
        if (bytes(sourceRef).length == 0) revert EmptySourceRef();
        if (itemCount == 0) revert InvalidItemCount();

        if (proofs[proofId].createdAt != 0) revert ProofAlreadyExists();

        proofs[proofId] = DocumentProof({
            payloadHash: payloadHash,
            createdAt: uint64(block.timestamp),
            itemCount: itemCount,
            issuer: msg.sender,
            docType: docType,
            sourceRef: sourceRef
        });

        emit DocumentProofRegistered(
            proofId,
            payloadHash,
            docType,
            sourceRef,
            itemCount,
            msg.sender,
            block.timestamp
        );
    }

    function getProof(
        bytes32 proofId
    ) external view returns (DocumentProof memory) {
        DocumentProof memory proof = proofs[proofId];
        if (proof.createdAt == 0) revert UnknownProof();
        return proof;
    }

    function verifyProof(
        bytes32 proofId,
        bytes32 payloadHash
    ) external view returns (bool) {
        DocumentProof memory proof = proofs[proofId];
        if (proof.createdAt == 0) return false;
        return proof.payloadHash == payloadHash;
    }
}
