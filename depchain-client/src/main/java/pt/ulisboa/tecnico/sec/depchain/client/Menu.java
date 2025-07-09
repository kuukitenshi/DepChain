package pt.ulisboa.tecnico.sec.depchain.client;

import java.math.BigInteger;
import java.util.Scanner;

import pt.ulisboa.tecnico.sec.depchain.client.exceptions.RevertException;
import pt.ulisboa.tecnico.sec.depchain.common.logging.ANSIColors;

public class Menu {

    private Scanner sc;
    private ClientStub clientStub;

    public Menu(Scanner sc, ClientStub clientStub) {
        this.sc = sc;
        this.clientStub = clientStub;
    }

    public void execMenu() {
        System.out.println("> =================================== Menu =================================== <");
        System.out.println("> Transactions:");
        System.out.println("    1 - Transfer (DepCoin)");
        System.out.println("> Contracts:");
        System.out.println("    2 - Blacklist");
        System.out.println("    3 - ISTCoin");
        System.out.println("> Attacks:");
        System.out.println("    4 - Resend last transaction request");
        System.out.println("> Quit:");
        System.out.println("    0 - Exit");
        System.out.println("> ============================================================================ <");
        System.out.print("$ Select an option: ");
        String option = this.sc.nextLine().trim();
        chooseOptionMenu(Integer.parseInt(option));
    }

    private int printBlacklistMenu() {
        System.out.println("> ---------------------------- Blacklist Functions --------------------------- <");
        System.out.println("> Blacklist:");
        System.out.println("    1 - Add address to blacklist <address>");
        System.out.println("    2 - Remove address from blacklist <address>");
        System.out.println("    3 - Check if address is blacklisted <address>");
        System.out.println("> Quit:");
        System.out.println("    0 - Return to main menu");
        System.out.println("> ---------------------------------------------------------------------------- <");
        System.out.print("$ Select an option: ");
        return Integer.parseInt(this.sc.nextLine().trim());
    }

    private int printISTMenu() {
        System.out.println("> ----------------------------- ISTCoin Functions ----------------------------- <");
        System.out.println("> ISTCoin:");
        System.out.println("    1 - Currency name");
        System.out.println("    2 - Currency symbol");
        System.out.println("    3 - Decimals");
        System.out.println("    4 - Total supply");
        System.out.println("    5 - Balance of <owner>");
        System.out.println("    6 - Transfer <receiver> <amount>");
        System.out.println("    7 - Transfer from <owner> <receiver> <amount>");
        System.out.println("    8 - Approve <spender> <amount>");
        System.out.println("    9 - Allowance <owner> <spender>");
        System.out.println("> Quit:");
        System.out.println("    0 - Return to main menu");
        System.out.println("> ---------------------------------------------------------------------------- <");
        System.out.print("$ Select an option: ");
        return Integer.parseInt(this.sc.nextLine().trim());
    }

    private void chooseOptionMenu(int option) {
        switch (option) {
            case 1:
                System.out.print("Insert receiver address: ");
                String receiver = sc.nextLine().trim();
                System.out.print("Insert amount to transfer: ");
                BigInteger amount = new BigInteger(sc.nextLine().trim());
                boolean wasExecuted = this.clientStub.executeExternal(receiver, amount);
                if (wasExecuted) {
                    System.out.println(ANSIColors.GREEN_FG
                            + String.format("External transfer of %s to %s was successful", amount, receiver)
                            + ANSIColors.RESET);
                } else {
                    System.out.println(ANSIColors.RED_FG + "Not enough balance to perform the external transaction!"
                            + ANSIColors.RESET);
                }
                break;
            case 2:
                chooseOptionBlacklist(printBlacklistMenu());
                break;
            case 3:
                chooseOptionIstCoin(printISTMenu());
                break;
            case 4:
                System.out.println(ANSIColors.YELLOW_FG + "(Replay Attack): Resending last transaction request..."
                        + ANSIColors.RESET);
                boolean wasResent = this.clientStub.replayAttack();
                if (wasResent) {
                    System.out.println(ANSIColors.GREEN_FG + "Last transaction request resent successfully!"
                            + ANSIColors.RESET);
                } else {
                    System.out.println(ANSIColors.RED_FG + "There is no previous transaction to send!"
                            + ANSIColors.RESET);
                }
                break;
            case 0:
                System.out.println("Exiting...");
                System.exit(0);
            default:
                System.out.println("Invalid option! Try again.");
                break;
        }
    }

    private void chooseOptionBlacklist(int option) {
        String address;
        try {
            switch (option) {
                case 1:
                    System.out.print("Insert address to add to blacklist: ");
                    address = this.sc.nextLine().trim();
                    boolean wasAdded = this.clientStub.addToBlackList(address);
                    if (wasAdded) {
                        System.out.println(ANSIColors.GREEN_FG
                                + String.format("Address %s was added to blacklist", address) + ANSIColors.RESET);
                    } else {
                        System.out
                                .println(ANSIColors.RED_FG + "Address was not added to blacklist!" + ANSIColors.RESET);
                    }
                    break;
                case 2:
                    System.out.print("Insert address to remove from blacklist: ");
                    address = this.sc.nextLine().trim();
                    boolean wasRemoved = this.clientStub.removeFromBlackList(address);
                    if (wasRemoved) {
                        System.out.println(ANSIColors.GREEN_FG
                                + String.format("Address %s was removed from blacklist", address) + ANSIColors.RESET);
                    } else {
                        System.out
                                .println(ANSIColors.RED_FG + "Address was not removed from blacklist!"
                                        + ANSIColors.RESET);
                    }
                    break;
                case 3:
                    System.out.print("Insert address to check if is blacklisted: ");
                    address = this.sc.nextLine().trim();
                    boolean isBlacklisted = this.clientStub.isBlackListed(address);
                    if (isBlacklisted) {
                        System.out.println(ANSIColors.GREEN_FG + String.format("Address %s is blacklisted", address)
                                + ANSIColors.RESET);
                    } else {
                        System.out.println(ANSIColors.RED_FG + String.format("Address %s is not blacklisted", address)
                                + ANSIColors.RESET);
                    }
                    break;
                case 4:
                    execMenu();
                    break;
                default:
                    System.out.println("Invalid option! Try again.");
                    break;
            }
        } catch (RevertException e) {
            System.out.println(
                    ANSIColors.RED_FG + "Client needs to be an admin to perform the operation!" + ANSIColors.RESET);
        }
    }

    private void chooseOptionIstCoin(int option) {

        switch (option) {
            case 1:
                String name = this.clientStub.name();
                if (name == null) {
                    System.out.println(ANSIColors.RED_FG + "Transaction name was not executed!" + ANSIColors.RESET);
                } else {
                    System.out
                            .println(ANSIColors.GREEN_FG + String.format("Currency name: %s", name)
                                    + ANSIColors.RESET);
                }
                break;
            case 2:
                String symbol = this.clientStub.symbol();
                if (symbol == null) {
                    System.out
                            .println(ANSIColors.RED_FG + "Transaction symbol was not executed!" + ANSIColors.RESET);
                } else {
                    System.out.println(
                            ANSIColors.GREEN_FG + String.format("Currency symbol: %s", symbol) + ANSIColors.RESET);
                }
                break;
            case 3:
                BigInteger decimals = this.clientStub.decimals();
                if (decimals == null) {
                    System.out.println(
                            ANSIColors.RED_FG + "Transaction decimals was not executed!" + ANSIColors.RESET);
                } else {
                    System.out
                            .println(ANSIColors.GREEN_FG + String.format("Decimals: %s", decimals)
                                    + ANSIColors.RESET);
                }
                break;
            case 4:
                BigInteger totalSupply = this.clientStub.totalSupply();
                if (totalSupply == null) {
                    System.out.println(
                            ANSIColors.RED_FG + "Transaction total supply was not executed!" + ANSIColors.RESET);
                } else {
                    System.out.println(
                            ANSIColors.GREEN_FG + String.format("Total supply: %s", totalSupply)
                                    + ANSIColors.RESET);
                }
                break;
            case 5:
                System.out.print("Insert owner address: ");
                String owner = this.sc.nextLine().trim();
                BigInteger balance = this.clientStub.balanceOf(owner);
                if (balance == null) {
                    System.out.println(
                            ANSIColors.RED_FG + "Transaction balance was not executed!" + ANSIColors.RESET);
                } else {
                    System.out.println(ANSIColors.GREEN_FG + String.format("Balance of %s: %s", owner, balance)
                            + ANSIColors.RESET);
                }
                break;
            case 6:
                System.out.print("Insert receiver address: ");
                String receiver = this.sc.nextLine().trim();
                System.out.print("Insert amount to transfer: ");
                BigInteger amount = new BigInteger(this.sc.nextLine().trim());
                boolean wasTransferred = false;
                try {
                    wasTransferred = this.clientStub.transfer(receiver, amount);
                } catch (RevertException e) {
                    System.out.println(ANSIColors.RED_FG + "Invalid address, not enough balance or blacklist!"
                            + ANSIColors.RESET);
                    break;
                }
                if (wasTransferred) {
                    System.out.println(
                            ANSIColors.GREEN_FG
                                    + String.format("Transfer of %s to %s was successful", amount, receiver)
                                    + ANSIColors.RESET);
                } else {
                    System.out.println(
                            ANSIColors.RED_FG + "Transaction transfer was not executed!" + ANSIColors.RESET);
                }
                break;
            case 7:
                System.out.print("Insert owner address: ");
                String from = this.sc.nextLine().trim();
                System.out.print("Insert receiver address: ");
                String to = this.sc.nextLine().trim();
                System.out.print("Insert amount to transfer: ");
                BigInteger value = new BigInteger(this.sc.nextLine().trim());
                boolean wasTransferredFrom = false;
                try {
                    wasTransferredFrom = this.clientStub.transferFrom(from, to, value);
                } catch (RevertException e) {
                    System.out.println(
                            ANSIColors.RED_FG + "Invalid address, not enough balance or allowance, or blacklist!"
                                    + ANSIColors.RESET);
                    break;
                }
                if (wasTransferredFrom) {
                    System.out.println(ANSIColors.GREEN_FG
                            + String.format("Transfer of %s from %s to %s was successful", value, from, to)
                            + ANSIColors.RESET);
                } else {
                    System.out.println(
                            ANSIColors.RED_FG + "Transaction transferFrom was not executed!" + ANSIColors.RESET);
                }
                break;
            case 8:
                System.out.print("Insert spender address: ");
                String spender = this.sc.nextLine().trim();
                System.out.print("Insert amount: ");
                BigInteger val = new BigInteger(this.sc.nextLine().trim());
                boolean wasApproved = false;
                try {
                    wasApproved = this.clientStub.approve(spender, val);
                } catch (RevertException e) {
                    System.out.println(ANSIColors.RED_FG + "Invalid address!"
                            + ANSIColors.RESET);
                    break;
                }
                if (wasApproved) {
                    System.out.println(ANSIColors.GREEN_FG
                            + String.format("Approval of %s to %s was successful", val, spender)
                            + ANSIColors.RESET);
                } else {
                    System.out.println(
                            ANSIColors.RED_FG + "Transaction approve was not executed!" + ANSIColors.RESET);
                }
                break;
            case 9:
                System.out.print("Insert owner address: ");
                String ownerAddress = this.sc.nextLine().trim();
                System.out.print("Insert spender address: ");
                String spend = this.sc.nextLine().trim();
                BigInteger allowance = this.clientStub.allowance(ownerAddress, spend);
                if (allowance == null) {
                    System.out
                            .println(ANSIColors.RED_FG + "Transaction allowance was not executed!"
                                    + ANSIColors.RESET);
                } else {
                    System.out.println(ANSIColors.GREEN_FG
                            + String.format("Allowance of %s to %s: %s", ownerAddress, spend, allowance)
                            + ANSIColors.RESET);
                }
                break;
            case 0:
                execMenu();
                break;
            default:
                System.out.println("Invalid option! Try again.");
                break;
        }
    }

}
