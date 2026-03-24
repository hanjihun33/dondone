// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "forge-std/Test.sol";

import "../src/DemoStableToken.sol";
import "../src/StableVault.sol";

contract StableVaultTest is Test {
    DemoStableToken private token;
    StableVault private vault;
    address private depositor = address(0x1);
    address private other = address(0x2);

    function setUp() public {
        token = new DemoStableToken();
        vault = new StableVault(address(token));

        token.mint(depositor, 1_000_000e6);
        vm.prank(depositor);
        token.approve(address(vault), type(uint256).max);
    }

    function testDepositMintsInitialSharesOneToOne() public {
        vm.prank(depositor);
        uint256 mintedShares = vault.deposit(100e6, depositor);

        assertEq(mintedShares, 100e6);
        assertEq(vault.totalShares(), 100e6);
        assertEq(vault.totalAssets(), 100e6);
        assertEq(vault.balanceOf(depositor), 100e6);
    }

    function testWithdrawBurnsSharesAndReturnsAssets() public {
        vm.prank(depositor);
        vault.deposit(200e6, depositor);

        uint256 balanceBefore = token.balanceOf(depositor);

        vm.prank(depositor);
        uint256 burnedShares = vault.withdraw(50e6, depositor, depositor);

        assertEq(burnedShares, 50e6);
        assertEq(vault.totalShares(), 150e6);
        assertEq(vault.totalAssets(), 150e6);
        assertEq(vault.balanceOf(depositor), 150e6);
        assertEq(token.balanceOf(depositor), balanceBefore + 50e6);
    }

    function testApprovedOperatorCanWithdrawForShareOwner() public {
        vm.prank(depositor);
        vault.deposit(300e6, depositor);

        vm.prank(depositor);
        vault.approveShares(other, 100e6);

        vm.prank(other);
        uint256 burnedShares = vault.withdraw(100e6, other, depositor);

        assertEq(burnedShares, 100e6);
        assertEq(vault.balanceOf(depositor), 200e6);
        assertEq(token.balanceOf(other), 100e6);
    }
}
