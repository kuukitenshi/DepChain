// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import {IAccessControl} from "./IAccessControl.sol";

contract AccessControl is IAccessControl {
    mapping(address account => bool) private _blacklist;
    mapping(address account => bool) private _admins;

    function addToBlacklist(address account) public returns (bool) {
        require(_admins[msg.sender], "User must be admin!");
        _blacklist[account] = true;
        return true;
    }

    function removeFromBlacklist(address account) public returns (bool) {
        require(_admins[msg.sender], "User must be admin!");
        _blacklist[account] = false;
        return true;
    }

    function isBlacklisted(address account) public view returns (bool) {
        return _blacklist[account];
    }
}
