package info.kgeorgiy.ja.shcherbakov.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Person extends Remote {
    String getFirstName() throws RemoteException;

    String getLastName() throws RemoteException;

    String getPassportNumber() throws RemoteException;

    Account createAccount(String subId) throws RemoteException;

    Account getAccount(String subId) throws RemoteException;
}
