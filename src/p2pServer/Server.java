/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p2pServer;

/**
 *
 * @author tranl
 */
import java.rmi.*;
import java.util.List;

//interface for index server
public interface Server extends java.rmi.Remote {
    //registry method

    public boolean registry(Integer peerId, String filename)
            throws RemoteException;

    //search method
    public List search(String filename)
            throws RemoteException;

}
