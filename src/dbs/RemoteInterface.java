package dbs;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {
    void backup(String filepath, int replicationDeg) throws RemoteException;
    void restore(String filepath) throws RemoteException;
    void delete(String pathname) throws RemoteException;
    int reclaim(int maxSize) throws RemoteException;
    String state() throws RemoteException;
}