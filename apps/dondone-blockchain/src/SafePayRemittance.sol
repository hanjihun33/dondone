// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

interface IERC20Like {
    function transferFrom(
        address from,
        address to,
        uint256 amount
    ) external returns (bool);
}

contract SafePayRemittance {
    enum TransferStatus {
        NONE,
        REQUESTED,
        SUBMITTED,
        CONFIRMED,
        FAILED
    }

    struct RecipientPolicy {
        bool allowed;
        uint64 updatedAt;
    }

    struct TransferRequest {
        uint256 id;
        address sender;
        address recipient;
        uint256 amount;
        bytes32 idempotencyHash;
        uint64 requestedAt;
        uint64 updatedAt;
        TransferStatus status;
        bytes32 lastTxHash;
        string failureCode;
    }

    // uint256 public constant COOLDOWN_SECONDS = 24 hours;
    uint256 public constant COOLDOWN_SECONDS = 1 minutes;

    IERC20Like public immutable token;
    address public owner;
    uint256 public highAmountThreshold;

    mapping(address => bool) public operators;
    mapping(address => mapping(address => RecipientPolicy)) public recipientPolicies;
    mapping(uint256 => TransferRequest) private requests;
    mapping(bytes32 => uint256) public requestIdByIdempotencyHash;

    uint256 public nextRequestId = 1;

    event OwnershipTransferred(address indexed previousOwner, address indexed newOwner);
    event OperatorUpdated(address indexed operator, bool allowed);
    event HighAmountThresholdUpdated(uint256 previousThreshold, uint256 newThreshold);
    event RecipientPolicyUpdated(
        address indexed sender, address indexed recipient, bool allowed, uint256 updatedAt
    );
    event TransferRequested(
        uint256 indexed requestId,
        address indexed sender,
        address indexed recipient,
        uint256 amount,
        bytes32 idempotencyHash
    );
    event TransferStatusChanged(
        uint256 indexed requestId, TransferStatus status, bytes32 txHash, string failureCode
    );

    error NotOwner();
    error NotOperator();
    error InvalidAddress();
    error InvalidAmount();
    error InvalidTxHash();
    error EmptyIdempotencyKey();
    error EmptyFailureCode();
    error PolicyBlocked(string code);
    error InvalidStatusTransition(TransferStatus currentStatus, TransferStatus nextStatus);
    error UnknownRequest();
    error TokenTransferFailed();

    modifier onlyOwner() {
        if (msg.sender != owner) revert NotOwner();
        _;
    }

    modifier onlyOperator() {
        if (!operators[msg.sender]) revert NotOperator();
        _;
    }

    constructor(
        address tokenAddress,
        uint256 initialHighAmountThreshold
    ) {
        if (tokenAddress == address(0)) revert InvalidAddress();

        token = IERC20Like(tokenAddress);
        owner = msg.sender;
        operators[msg.sender] = true;
        highAmountThreshold = initialHighAmountThreshold;

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

    function setHighAmountThreshold(
        uint256 newThreshold
    ) external onlyOwner {
        emit HighAmountThresholdUpdated(highAmountThreshold, newThreshold);
        highAmountThreshold = newThreshold;
    }

    function setRecipientAllow(
        address recipient,
        bool allowed
    ) external {
        if (recipient == address(0)) revert InvalidAddress();

        RecipientPolicy storage policy = recipientPolicies[msg.sender][recipient];
        policy.allowed = allowed;
        policy.updatedAt = uint64(block.timestamp);

        emit RecipientPolicyUpdated(msg.sender, recipient, allowed, block.timestamp);
    }

    function requestTransfer(
        address recipient,
        uint256 amount,
        string calldata idempotencyKey,
        bool highAmountConfirmed
    ) external returns (uint256 requestId) {
        if (recipient == address(0)) revert InvalidAddress();
        if (amount == 0) revert InvalidAmount();
        if (bytes(idempotencyKey).length == 0) revert EmptyIdempotencyKey();

        bytes32 idempotencyHash = keccak256(abi.encode(msg.sender, idempotencyKey));
        uint256 existingId = requestIdByIdempotencyHash[idempotencyHash];
        if (existingId != 0) {
            return existingId;
        }

        (bool allowedNow, string memory code,) =
            canTransferNow(msg.sender, recipient, amount, highAmountConfirmed);
        if (!allowedNow) revert PolicyBlocked(code);

        requestId = nextRequestId++;

        TransferRequest storage request = requests[requestId];
        request.id = requestId;
        request.sender = msg.sender;
        request.recipient = recipient;
        request.amount = amount;
        request.idempotencyHash = idempotencyHash;
        request.requestedAt = uint64(block.timestamp);
        request.updatedAt = uint64(block.timestamp);
        request.status = TransferStatus.REQUESTED;

        requestIdByIdempotencyHash[idempotencyHash] = requestId;

        emit TransferRequested(requestId, msg.sender, recipient, amount, idempotencyHash);
        emit TransferStatusChanged(requestId, TransferStatus.REQUESTED, bytes32(0), "");
    }

    function markSubmitted(
        uint256 requestId,
        bytes32 txHash
    ) external onlyOperator {
        if (txHash == bytes32(0)) revert InvalidTxHash();

        TransferRequest storage request = _getExistingRequest(requestId);
        if (request.status != TransferStatus.REQUESTED) {
            revert InvalidStatusTransition(request.status, TransferStatus.SUBMITTED);
        }

        request.status = TransferStatus.SUBMITTED;
        request.lastTxHash = txHash;
        request.updatedAt = uint64(block.timestamp);

        emit TransferStatusChanged(requestId, TransferStatus.SUBMITTED, txHash, "");
    }

    function markConfirmed(
        uint256 requestId,
        bytes32 txHash
    ) external onlyOperator {
        if (txHash == bytes32(0)) revert InvalidTxHash();

        TransferRequest storage request = _getExistingRequest(requestId);

        if (request.status != TransferStatus.SUBMITTED) {
            revert InvalidStatusTransition(request.status, TransferStatus.CONFIRMED);
        }

        request.status = TransferStatus.CONFIRMED;
        request.lastTxHash = txHash;
        request.updatedAt = uint64(block.timestamp);

        bool success = token.transferFrom(request.sender, request.recipient, request.amount);
        if (!success) revert TokenTransferFailed();

        emit TransferStatusChanged(requestId, TransferStatus.CONFIRMED, txHash, "");
    }

    function markFailed(
        uint256 requestId,
        string calldata failureCode
    ) external onlyOperator {
        if (bytes(failureCode).length == 0) revert EmptyFailureCode();

        TransferRequest storage request = _getExistingRequest(requestId);

        if (request.status != TransferStatus.SUBMITTED) {
            revert InvalidStatusTransition(request.status, TransferStatus.FAILED);
        }

        request.status = TransferStatus.FAILED;
        request.failureCode = failureCode;
        request.updatedAt = uint64(block.timestamp);

        emit TransferStatusChanged(requestId, TransferStatus.FAILED, bytes32(0), failureCode);
    }

    function canTransferNow(
        address sender,
        address recipient,
        uint256 amount,
        bool highAmountConfirmed
    ) public view returns (bool allowedNow, string memory blockedCode, uint256 waitSeconds) {
        RecipientPolicy memory policy = recipientPolicies[sender][recipient];

        if (!policy.allowed) {
            return (false, "RECIPIENT_NOT_ALLOWED", 0);
        }

        uint256 unlockedAt = uint256(policy.updatedAt) + COOLDOWN_SECONDS;
        if (block.timestamp < unlockedAt) {
            return (false, "COOLDOWN_ACTIVE", unlockedAt - block.timestamp);
        }

        if (amount > highAmountThreshold && !highAmountConfirmed) {
            return (false, "HIGH_AMOUNT_CONFIRM_REQUIRED", 0);
        }

        return (true, "", 0);
    }

    function getPolicy(
        address sender,
        address recipient
    ) external view returns (RecipientPolicy memory policy, uint256 cooldownEndsAt) {
        policy = recipientPolicies[sender][recipient];
        cooldownEndsAt = uint256(policy.updatedAt) + COOLDOWN_SECONDS;
    }

    function getTransferRequest(
        uint256 requestId
    ) external view returns (TransferRequest memory) {
        return _getExistingRequest(requestId);
    }

    function getTransferReceiptHash(
        uint256 requestId
    ) external view returns (bytes32) {
        TransferRequest storage request = _getExistingRequest(requestId);
        return keccak256(
            abi.encode(
                block.chainid,
                address(this),
                request.id,
                request.sender,
                request.recipient,
                request.amount,
                request.status,
                request.lastTxHash,
                request.failureCode,
                request.requestedAt,
                request.updatedAt,
                request.idempotencyHash
            )
        );
    }

    function _getExistingRequest(
        uint256 requestId
    ) internal view returns (TransferRequest storage request) {
        request = requests[requestId];
        if (request.id == 0) revert UnknownRequest();
    }
}
