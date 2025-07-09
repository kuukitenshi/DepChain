// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import {ERC20} from "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import {IAccessControl} from "./IAccessControl.sol";

contract ISTCoin is ERC20 {

    IAccessControl private accessControlContract;

    constructor() ERC20("IST Coin", "IST") {
    }

    function name() public pure override returns (string memory) {
        return "IST Coin";
    }

    function symbol() public pure override returns (string memory) {
        return "IST";
    }

    function decimals() public pure override returns (uint8) {
        return 2;
    }

    function transfer(address to, uint256 value) public override returns (bool success) {
        require(!accessControlContract.isBlacklisted(msg.sender), "User is blacklisted!");
        _transfer(msg.sender, to, value);
        return true;
    }

    function transferFrom(address from, address to, uint256 value) public override returns (bool success) {
        require(!accessControlContract.isBlacklisted(msg.sender), "User is blacklisted!");
        address spender = msg.sender;
        _spendAllowance(from, spender, value);
        _transfer(from, to, value);
        return true;
    }
}
