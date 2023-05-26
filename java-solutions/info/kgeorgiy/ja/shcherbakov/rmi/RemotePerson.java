package info.kgeorgiy.ja.shcherbakov.rmi;


public class RemotePerson extends AbstractPerson {
    public RemotePerson(Bank bank, String firstName, String lastName, String passportNumber) {
        super(bank, firstName, lastName, passportNumber);
    }
}
