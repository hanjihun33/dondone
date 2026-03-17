// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import { Script } from "forge-std/Script.sol";
import { console2 } from "forge-std/console2.sol";
import { DemoStableToken } from "../src/DemoStableToken.sol";
import { SafePayRemittance } from "../src/SafePayRemittance.sol";
import { DocumentHashRegistry } from "../src/DocumentHashRegistry.sol";

contract DeploySafePayScript is Script {
    function run() external {
        uint256 deployerKey = vm.envUint("PRIVATE_KEY");
        uint256 highAmountThreshold = vm.envOr("HIGH_AMOUNT_THRESHOLD", uint256(300e6));
        uint256 mintAmount = vm.envOr("MINT_AMOUNT", uint256(1_000_000e6));
        address treasury = vm.envOr("TREASURY", vm.addr(deployerKey));

        vm.startBroadcast(deployerKey);

        DemoStableToken token = new DemoStableToken("Demo USDC", "dUSDC", 6);
        SafePayRemittance safePay = new SafePayRemittance(address(token), highAmountThreshold);
        DocumentHashRegistry docRegistry = new DocumentHashRegistry();
        token.mint(treasury, mintAmount);

        vm.stopBroadcast();

        console2.log("DemoStableToken:", address(token));
        console2.log("SafePayRemittance:", address(safePay));
        console2.log("DocumentHashRegistry:", address(docRegistry));
        console2.log("Treasury:", treasury);
    }
}
