package info.kgeorgiy.ja.shcherbakov.rmi;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

@RunWith(JUnit4.class)

public class BankTest {
    protected static final Random RANDOM = new Random(2350238475230489753L);

    private static final int REGISTRY_PORT = 1099;
    private static final int BANK_PORT = 8088;
    private static final String BANK_URL = "bank";
    private Bank bank;
    private static Registry registry;

    @BeforeClass
    public static void runRegistry() throws RemoteException {
        System.out.println("Run registry");
        registry = LocateRegistry.createRegistry(REGISTRY_PORT);
    }

    @AfterClass
    public static void shutdownRegistry() {
        System.out.println("Shutdown registry");
        try {
            UnicastRemoteObject.unexportObject(registry, true);
        } catch (NoSuchObjectException ignored) {
        }
    }

    @Before
    public void runServer() throws RemoteException {
        System.out.println("Prepare environment...");
        bank = new RemoteBank(BANK_PORT);
        UnicastRemoteObject.exportObject(bank, BANK_PORT);
        registry.rebind(BANK_URL, bank);
    }

    @After
    public void shutdownServer() throws RemoteException {
        System.out.println("Shutdown environment...");
        try {
            registry.unbind(BANK_URL);
            UnicastRemoteObject.unexportObject(bank, true);
        } catch (NoSuchObjectException | NotBoundException ignored) {
        }

    }

    // BankTests
    @Test
    public void createAccount() throws RemoteException {
        String id = randomString();
        Account account = bank.createAccount(id);
        Assert.assertNotNull(account);
        Assert.assertEquals(id, account.getId());
        account.setAmount(50);
        Assert.assertEquals(50, account.getAmount());
    }

    @Test
    public void createAndGetAccount() throws RemoteException {
        String id = randomString();
        Account created = bank.createAccount(id);
        Account got = bank.getAccount(id);
        Assert.assertSame(got, created);
        Assert.assertEquals(got, created);

        created.setAmount(50);
        Assert.assertEquals(got, created);

        got.setAmount(100);
        Assert.assertEquals(got, created);
    }


    @Test
    public void getNotExistingAccount() throws RemoteException {
        Assert.assertNull(bank.getAccount(randomString()));
    }

    @Test
    public void createPerson() throws RemoteException {
        Assert.assertNotNull(createRandomPerson());
    }

    @Test
    public void createAndGetRemotePerson() throws RemoteException {
        Person created = createRandomPerson();
        Person got = bank.getRemotePerson(created.getPassportNumber());
        Assert.assertNotNull(created);
        Assert.assertNotNull(got);
        Assert.assertSame(created, got);
        Assert.assertEquals(created, got);
    }

    @Test
    public void getNotExistingRemotePerson() throws RemoteException {
        Assert.assertNull(bank.getRemotePerson(randomString()));
    }

    @Test
    public void createAndGetLocalPerson() throws RemoteException {
        Person created = createRandomPerson();
        Person local = bank.getLocalPerson(created.getPassportNumber());
        Assert.assertNotNull(created);
        Assert.assertNotNull(local);
        Assert.assertEquals(created, local);
    }

    @Test
    public void getNotExistingLocalPerson() throws RemoteException {
        Assert.assertNull(bank.getLocalPerson(randomString()));
    }

    @Test
    public void createPersonAccount() throws RemoteException {
        Person person = createRandomPerson();
        String subId = randomString();
        Account account = person.createAccount(subId);
        String inBankId = personAccountId(person, subId);
        Account getBank = bank.getAccount(inBankId);
        Account getPerson = person.getAccount(subId);
        Assert.assertNotNull(account);
        Assert.assertNotNull(getBank);
        Assert.assertNotNull(getPerson);
        Assert.assertSame(account, getBank);
        Assert.assertSame(account, getPerson);
        Assert.assertEquals(account, getBank);
        Assert.assertEquals(account, getPerson);
    }

    @Test
    public void setPositiveAmount() throws RemoteException {
        String id = randomString();
        Account account = bank.createAccount(id);
        int amount = RANDOM.nextInt(0, Integer.MAX_VALUE);
        account.setAmount(amount);
        Assert.assertEquals(amount, account.getAmount());
    }

    @Test
    public void setNegativeAmount() throws RemoteException {
        String id = randomString();
        Account account = bank.createAccount(id);
        int amount = RANDOM.nextInt(Integer.MIN_VALUE, 0);
        account.setAmount(amount);
        Assert.assertEquals(0, account.getAmount());
    }

    @Test
    public void setAmountRemote() throws RemoteException {
        Person person = createRandomPerson();
        String subId = randomString();
        Account account = person.createAccount(subId);
        account.setAmount(100);
        Person remoteBeforeView = bank.getRemotePerson(person.getPassportNumber());
        Person localBeforeView = bank.getLocalPerson(person.getPassportNumber());

        Assert.assertEquals(account.getAmount(), remoteBeforeView.getAccount(subId).getAmount());

        account.setAmount(200);

        Person remoteAfterView = bank.getRemotePerson(person.getPassportNumber());
        Person localAfterView = bank.getLocalPerson(person.getPassportNumber());

        Assert.assertEquals(account.getAmount(), remoteBeforeView.getAccount(subId).getAmount());
        Assert.assertEquals(account.getAmount(), remoteAfterView.getAccount(subId).getAmount());
        Assert.assertEquals(100, localBeforeView.getAccount(subId).getAmount());
        Assert.assertEquals(account.getAmount(), localAfterView.getAccount(subId).getAmount());
    }

    @Test
    public void setAmountLocal() throws RemoteException {
        Person remote = createRandomPerson();
        String subId = randomString();
        Account account = remote.createAccount(subId);
        account.setAmount(100);
        Person localBeforeView = bank.getLocalPerson(remote.getPassportNumber());

        Assert.assertEquals(100, localBeforeView.getAccount(subId).getAmount());

        localBeforeView.getAccount(subId).setAmount(200);

        Person remoteAfterView = bank.getRemotePerson(remote.getPassportNumber());
        Person localAfterView = bank.getLocalPerson(remote.getPassportNumber());

        Assert.assertEquals(100, remote.getAccount(subId).getAmount());
        Assert.assertEquals(100, remoteAfterView.getAccount(subId).getAmount());
        Assert.assertEquals(200, localBeforeView.getAccount(subId).getAmount());
        Assert.assertEquals(100, localAfterView.getAccount(subId).getAmount());
    }

    // Client tests
    @Test
    public void remoteClientNoPerson() throws RemoteException {
        String name = randomString(), surname = randomString(), passport = randomString(), accId = randomString();
        int amountDelta = RANDOM.nextInt(Integer.MAX_VALUE);
        RemoteClient.main(name, surname, passport, accId, String.valueOf(amountDelta));

        Person remote = bank.getRemotePerson(passport);

        Assert.assertNotNull(remote);
        Assert.assertEquals(remote.getFirstName(), name);
        Assert.assertEquals(remote.getLastName(), surname);
        Assert.assertEquals(remote.getPassportNumber(), passport);

        Account account = remote.getAccount(accId);

        Assert.assertNotNull(account);
        Assert.assertEquals(amountDelta, account.getAmount());
    }

    @Test
    public void remoteClientNoAccount() throws RemoteException {
        String name = randomString(), surname = randomString(), passport = randomString(), accId = randomString();
        int amountDelta = RANDOM.nextInt(Integer.MAX_VALUE);
        Person origin = bank.createPerson(name, surname, passport);
        RemoteClient.main(name, surname, passport, accId, String.valueOf(amountDelta));

        Person remote = bank.getRemotePerson(passport);

        Assert.assertNotNull(remote);
        Assert.assertSame(origin, remote);
        Assert.assertEquals(remote.getFirstName(), name);
        Assert.assertEquals(remote.getLastName(), surname);
        Assert.assertEquals(remote.getPassportNumber(), passport);

        Account account = remote.getAccount(accId);

        Assert.assertNotNull(account);
        Assert.assertEquals(amountDelta, account.getAmount());
    }

    @Test
    public void remoteClientWrongPersonData() throws RemoteException {
        String name = randomString(), surname = randomString(), passport = randomString(), accId = randomString();
        Person origin = bank.createPerson(name, surname, passport);
        RemoteClient.main(name + "!@#", surname + "AAA", passport, accId, "0");

        Person remote = bank.getRemotePerson(passport);

        Assert.assertNotNull(remote);
        Assert.assertSame(origin, remote);
        Assert.assertEquals(remote.getFirstName(), name);
        Assert.assertEquals(remote.getLastName(), surname);
        Assert.assertEquals(remote.getPassportNumber(), passport);

        Account account = remote.getAccount(accId);

        Assert.assertNull(account);
    }

    @Test
    public void remoteClientHaveAccount() throws RemoteException {
        String name = randomString(), surname = randomString(), passport = randomString(), accId = randomString();
        int startedAmount = RANDOM.nextInt(Integer.MAX_VALUE), amountDelta = RANDOM.nextInt();
        Person origin = bank.createPerson(name, surname, passport);
        Account account = origin.createAccount(accId);
        account.setAmount(startedAmount);
        RemoteClient.main(name, surname, passport, accId, String.valueOf(amountDelta));

        Person remote = bank.getRemotePerson(passport);

        Assert.assertNotNull(remote);
        Assert.assertSame(origin, remote);
        Assert.assertEquals(remote.getFirstName(), name);
        Assert.assertEquals(remote.getLastName(), surname);
        Assert.assertEquals(remote.getPassportNumber(), passport);

        Assert.assertEquals(Integer.max(startedAmount + amountDelta, 0), account.getAmount());
    }

    @Test
    public void localClient() throws RemoteException {
        String name = randomString(), surname = randomString(), passport = randomString(), accId = randomString();
        int startedAmount = RANDOM.nextInt(Integer.MAX_VALUE), amountDelta = RANDOM.nextInt();
        Person origin = bank.createPerson(name, surname, passport);
        Person localBefore = bank.getLocalPerson(passport);
        Account account = origin.createAccount(accId);
        account.setAmount(startedAmount);
        LocalClient.main(name, surname, passport, accId, String.valueOf(amountDelta));

        Person remote = bank.getRemotePerson(passport);

        Assert.assertSame(origin, remote);
        Assert.assertEquals(origin, remote);
        Assert.assertEquals(remote.getFirstName(), name);
        Assert.assertEquals(remote.getLastName(), surname);
        Assert.assertEquals(remote.getPassportNumber(), passport);
        Assert.assertEquals(origin, localBefore);

        Assert.assertEquals(startedAmount, account.getAmount());
    }


    private static String personAccountId(Person person, String subId) throws RemoteException {
        return person.getPassportNumber() + ":" + subId;
    }

    private static String randomString() {
        return String.valueOf(RANDOM.nextInt());
    }

    private Person createRandomPerson() throws RemoteException {
        return bank.createPerson(randomString(), randomString(), randomString());
    }


}
