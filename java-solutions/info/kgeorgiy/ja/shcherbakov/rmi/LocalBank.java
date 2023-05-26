package info.kgeorgiy.ja.shcherbakov.rmi;

import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LocalBank extends AbstractBank {

    private final Person owner;

    public Person getOwner() {
        return owner;
    }

    public LocalBank(Person person, ConcurrentMap<String, Account> personAccounts) throws RemoteException {
        super();
        Person localPerson = new LocalPerson(this, person.getFirstName(), person.getLastName(), person.getPassportNumber());
        this.owner = localPerson;
        this.personAccounts.put(localPerson, new ConcurrentHashMap<>());
        for (Account account : personAccounts.values()) {
            String id = account.getId();
            Account localAccount = new RemoteAccount(account);
            this.accounts.put(id, localAccount);
            this.personAccounts.get(localPerson).put(id, localAccount);
        }
    }

    @Override
    protected Account createNewAccount(final String id) throws RemoteException {
        return new RemoteAccount(id);
    }

    @Override
    public Person createPerson(String firstName, String lastName, String passportNumber) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Person getRemotePerson(String passportNumber) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Person getLocalPerson(String passportNumber) {
        throw new UnsupportedOperationException();
    }
}
