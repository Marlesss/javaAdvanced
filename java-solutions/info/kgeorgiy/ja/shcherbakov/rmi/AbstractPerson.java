package info.kgeorgiy.ja.shcherbakov.rmi;

import java.io.Serializable;
import java.rmi.RemoteException;

public abstract class AbstractPerson implements Person, Serializable {
    final private Bank bank;
    final private String firstName, lastName, passportNumber;

    public AbstractPerson(Bank bank, String firstName, String lastName, String passportNumber) {
        this.bank = bank;
        this.firstName = firstName;
        this.lastName = lastName;
        this.passportNumber = passportNumber;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getPassportNumber() {
        return passportNumber;
    }

    @Override
    public Account createAccount(final String subId) throws RemoteException {
        return bank.createAccount(this, subId);
    }

    @Override
    public Account getAccount(final String subId) throws RemoteException {
        return bank.getAccount(this, subId);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof AbstractPerson person) {
            return getFirstName().equals(person.getFirstName()) &&
                    getLastName().equals(person.getLastName()) &&
                    getPassportNumber().equals(person.getPassportNumber());
        }
        return false;
    }
}
