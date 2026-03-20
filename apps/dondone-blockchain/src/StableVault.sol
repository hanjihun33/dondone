// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

interface IERC20VaultAsset {
    function balanceOf(address account) external view returns (uint256);
    function transfer(address to, uint256 amount) external returns (bool);
    function transferFrom(address from, address to, uint256 amount) external returns (bool);
}

contract StableVault {
    IERC20VaultAsset public immutable asset;
    address public owner;
    bool public paused;

    uint256 public totalShares;
    mapping(address => uint256) private shareBalances;
    mapping(address => mapping(address => uint256)) public shareAllowances;

    uint256 private locked = 1;

    event OwnershipTransferred(address indexed previousOwner, address indexed newOwner);
    event Paused(address indexed account);
    event Unpaused(address indexed account);
    event Deposit(address indexed caller, address indexed receiver, uint256 assets, uint256 shares);
    event Withdraw(
        address indexed caller,
        address indexed receiver,
        address indexed shareOwner,
        uint256 assets,
        uint256 shares
    );
    event ShareApproval(address indexed owner, address indexed spender, uint256 amount);

    error NotOwner();
    error VaultPaused();
    error InvalidAddress();
    error InvalidAmount();
    error InsufficientShares();
    error InsufficientAllowance();
    error AssetTransferFailed();

    modifier onlyOwner() {
        if (msg.sender != owner) revert NotOwner();
        _;
    }

    modifier whenNotPaused() {
        if (paused) revert VaultPaused();
        _;
    }

    modifier nonReentrant() {
        require(locked == 1, "reentrant");
        locked = 2;
        _;
        locked = 1;
    }

    constructor(address assetAddress) {
        if (assetAddress == address(0)) revert InvalidAddress();

        asset = IERC20VaultAsset(assetAddress);
        owner = msg.sender;

        emit OwnershipTransferred(address(0), msg.sender);
    }

    function transferOwnership(address newOwner) external onlyOwner {
        if (newOwner == address(0)) revert InvalidAddress();

        emit OwnershipTransferred(owner, newOwner);
        owner = newOwner;
    }

    function pause() external onlyOwner {
        paused = true;
        emit Paused(msg.sender);
    }

    function unpause() external onlyOwner {
        paused = false;
        emit Unpaused(msg.sender);
    }

    function balanceOf(address account) external view returns (uint256) {
        return shareBalances[account];
    }

    function totalAssets() public view returns (uint256) {
        return asset.balanceOf(address(this));
    }

    function previewDeposit(uint256 assets) public view returns (uint256) {
        if (assets == 0) revert InvalidAmount();

        uint256 currentTotalShares = totalShares;
        uint256 currentTotalAssets = totalAssets();

        if (currentTotalShares == 0 || currentTotalAssets == 0) {
            return assets;
        }

        return (assets * currentTotalShares) / currentTotalAssets;
    }

    function previewWithdraw(uint256 assets) public view returns (uint256) {
        if (assets == 0) revert InvalidAmount();

        uint256 currentTotalShares = totalShares;
        uint256 currentTotalAssets = totalAssets();

        if (currentTotalShares == 0 || currentTotalAssets == 0) {
            revert InvalidAmount();
        }

        uint256 shares = (assets * currentTotalShares) / currentTotalAssets;
        if ((shares * currentTotalAssets) / currentTotalShares < assets) {
            shares += 1;
        }
        return shares;
    }

    function approveShares(address spender, uint256 amount) external returns (bool) {
        if (spender == address(0)) revert InvalidAddress();

        shareAllowances[msg.sender][spender] = amount;
        emit ShareApproval(msg.sender, spender, amount);
        return true;
    }

    function deposit(uint256 assets, address receiver)
        external
        whenNotPaused
        nonReentrant
        returns (uint256 shares)
    {
        if (receiver == address(0)) revert InvalidAddress();

        shares = previewDeposit(assets);

        bool success = asset.transferFrom(msg.sender, address(this), assets);
        if (!success) revert AssetTransferFailed();

        totalShares += shares;
        shareBalances[receiver] += shares;

        emit Deposit(msg.sender, receiver, assets, shares);
    }

    function withdraw(uint256 assets, address receiver, address shareOwner)
        external
        whenNotPaused
        nonReentrant
        returns (uint256 shares)
    {
        if (receiver == address(0) || shareOwner == address(0)) revert InvalidAddress();

        shares = previewWithdraw(assets);

        if (msg.sender != shareOwner) {
            uint256 allowed = shareAllowances[shareOwner][msg.sender];
            if (allowed < shares) revert InsufficientAllowance();
            if (allowed != type(uint256).max) {
                shareAllowances[shareOwner][msg.sender] = allowed - shares;
                emit ShareApproval(shareOwner, msg.sender, shareAllowances[shareOwner][msg.sender]);
            }
        }

        uint256 ownerShares = shareBalances[shareOwner];
        if (ownerShares < shares) revert InsufficientShares();

        shareBalances[shareOwner] = ownerShares - shares;
        totalShares -= shares;

        bool success = asset.transfer(receiver, assets);
        if (!success) revert AssetTransferFailed();

        emit Withdraw(msg.sender, receiver, shareOwner, assets, shares);
    }
}
