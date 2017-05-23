/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

/**
 *
 * @author tranl
 */
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.*;

//interface for Clientserver
public interface ClientServer extends java.rmi.Remote {

	//method to return file from client
	public byte[] obtain(String file, int peer, String instanceName) 
		throws RemoteException, FileNotFoundException, IOException;
	
	
}