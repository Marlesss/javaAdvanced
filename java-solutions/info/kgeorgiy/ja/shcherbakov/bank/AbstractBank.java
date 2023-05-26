package info.kgeorgiy.ja.shcherbakov.bank;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractBank implements Bank, Serializable {
    protected final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    protected final ConcurrentMap<Person, ConcurrentMap<String, Account>> personAccounts = new ConcurrentHashMap<>();
    protected final ConcurrentMap<String, Person> persons = new ConcurrentHashMap<>();

    protected static String personAccountId(Person person, String subId) throws RemoteException {
        return person.getPassportNumber() + ":" + subId;
    }

    protected ConcurrentMap<String, Account> getPersonAccounts(Person person) throws RemoteException {
        return personAccounts.computeIfAbsent(person, p -> new ConcurrentHashMap<>());
    }

    public Account createAccount(Person person, String subId) throws RemoteException {
        String id = personAccountId(person, subId);
        Account account = createAccount(id);
        getPersonAccounts(person).putIfAbsent(id, account);
        return account;
    }

    @Override
    public final Account createAccount(final String id) throws RemoteException {
        Account account = getAccount(id);
        if (account != null) {
            return account;
        }
        System.out.println("Creating account " + id);
        account = createNewAccount(id);
        accounts.put(id, account);
        return account;
    }

    protected abstract Account createNewAccount(final String id) throws RemoteException;

    @Override
    public Account getAccount(final String id) {
        System.out.println("Retrieving account " + id);
        return accounts.get(id);
    }

    public Account getAccount(Person person, String subId) throws RemoteException {
        return getAccount(personAccountId(person, subId));
    }
}
