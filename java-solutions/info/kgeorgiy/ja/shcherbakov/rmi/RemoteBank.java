package info.kgeorgiy.ja.shcherbakov.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public final class RemoteBank extends AbstractBank {
    private final int port;

    public RemoteBank(final int port) {
        super();
        this.port = port;
    }

    @Override
    protected Account createNewAccount(final String id) throws RemoteException {
        final Account account = new RemoteAccount(id);
        UnicastRemoteObject.exportObject(account, port);
        return account;
    }

    @Override
    public Person createPerson(String firstName, String lastName, String passportNumber) throws RemoteException {
        System.out.println("Creating person " + passportNumber);
        final Person person = new RemotePerson(this, firstName, lastName, passportNumber);
        if (persons.putIfAbsent(passportNumber, person) == null) {
            UnicastRemoteObject.exportObject(person, port);
            return person;
        } else {
            return getRemotePerson(passportNumber);
        }
    }

    @Override
    public Person getRemotePerson(String passportNumber) throws RemoteException {
        System.out.println("Retrieving person " + passportNumber);
        return persons.get(passportNumber);
    }

    @Override
    public Person getLocalPerson(String passportNumber) throws RemoteException {
        System.out.println("Retrieving local person " + passportNumber);
        Person person = getRemotePerson(passportNumber);
        if (person == null) {
            return null;
        }
        return new LocalBank(person, getPersonAccounts(person)).getOwner();
    }
}
