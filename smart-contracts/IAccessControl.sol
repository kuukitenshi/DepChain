// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

interface IAccessControl {
    function addToBlacklist(address account) external returns (bool);

    function removeFromBlacklist(address account) external returns (bool);

    function isBlacklisted(address account) external view returns (bool);
}
