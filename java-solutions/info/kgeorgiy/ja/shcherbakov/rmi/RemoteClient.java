package info.kgeorgiy.ja.shcherbakov.rmi;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Objects;

public final class RemoteClient {
    /**
     * Utility class.
     */
    private RemoteClient() {
    }

    public static void main(final String... args) throws RemoteException {
        final Bank bank = AbstractClient.getBank("//localhost:1099/bank");
        if (bank == null) {
            return;
        }
        if (args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Wrong arguments");
            System.err.println("RemoteClient name surname passportNumber accountId amountDelta");
            return;
        }
        try {
            final String name = args[0];
            final String surname = args[1];
            final String passport = args[2];
            final String accountId = args[3];
            final int amountDelta = Integer.parseInt(args[4]);
            AbstractClient.workClient(bank, false, name, surname, passport, accountId, amountDelta);
        } catch (NumberFormatException e) {
            System.err.println("amountDelta must be integer");
            System.err.println("RemoteClient name surname passportNumber accountId amountDelta");
            System.err.println(e.getMessage());
        }
    }
}
