package info.kgeorgiy.ja.shcherbakov.bank;

import java.io.Serializable;
import java.rmi.*;

public interface Account extends Remote, Serializable {
    /** Returns account identifier. */
    String getId() throws RemoteException;

    /** Returns amount of money in the account. */
    int getAmount() throws RemoteException;

    /** Sets amount of money in the account. */
    void setAmount(int amount) throws RemoteException;
}
