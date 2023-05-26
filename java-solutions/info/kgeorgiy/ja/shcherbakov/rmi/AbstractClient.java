package info.kgeorgiy.ja.shcherbakov.rmi;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class AbstractClient {
    /**
     * Utility class.
     */
    private AbstractClient() {
    }

    public static Bank getBank(String url) throws RemoteException {
        try {
            return (Bank) Naming.lookup(url);
        } catch (final NotBoundException e) {
            System.out.println("Bank is not bound");
        } catch (final MalformedURLException e) {
            System.out.println("Bank URL is invalid");
        }
        return null;
    }

    public static void workClient(Bank bank, boolean local, String name, String surname, String passport, String accountId, int amountDelta) throws RemoteException {
        Person person = getPerson(bank, local, passport);
        if (person == null) {
            System.out.println("Person not found");
            System.out.println("Creating person");
            bank.createPerson(name, surname, passport);
            person = getPerson(bank, local, passport);
        } else {
            System.out.println("Person found");
            System.out.println("Personal data verification");
            if (!(verifyString(person.getFirstName(), name, "There is a different name in the Person data!")
                    && verifyString(person.getLastName(), surname, "There is a different surname in the Person data!"))) {
                return;
            }
        }
        System.out.println("Getting account");
        Account account = person.getAccount(accountId);
        if (account == null) {
            System.out.println("Account not found");
            System.out.println("Creating account");
            account = person.createAccount(accountId);
            account.setAmount(0);
        }
        int amount = account.getAmount();
        System.out.println("Money: " + amount);
        account.setAmount(amount + amountDelta);
        System.out.println("Money: " + account.getAmount());
    }

    private static Person getPerson(Bank bank, boolean local, String passport) throws RemoteException {
        if (local) {
            System.out.println("Retrieving a local person");
            return bank.getLocalPerson(passport);
        } else {
            System.out.println("Retrieving a remote person");
            return bank.getRemotePerson(passport);
        }
    }

    private static boolean verifyString(String test, String required, String message) {
        if (!required.equals(test)) {
            System.out.println(message);
            return false;
        }
        return true;
    }
}
