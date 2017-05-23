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
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.Naming;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import p2pServer.*;

public class Client extends UnicastRemoteObject implements ClientServer {

    // peer identifier
    static Integer PeerId;
    static long avgResponseTime = 0;
    static long aggregateResponseTime = 0;
    static int numLookups = 0;

    // constructor
    public Client() throws RemoteException {
        super();
    }

    /**
     * Entry point for Client-Server
     *
     * Sets up peer id from command line argument and sets up instance name
     * Checks for shared folder and creates one if it doesn't exist Sets up
     * server for client connections Connects to index server Automatically
     * registers files in shared directory with server Enters program UI loop
     * Unbinds and exits
     *
     * @param args
     * @throws java.lang.Exception
     */
    public static void main(String args[]) throws Exception {
        // gets peerid from command line argument
//        if (args.length == 0) {

//            return;
//        }
        try {
            System.out.println("Please enter a peer id as a command line argument : ");
            Scanner io = new Scanner(System.in);
            PeerId = Integer.parseInt(io.nextLine());
        } catch (NumberFormatException e) {
            System.err.println("Argument must be an integer");
            System.exit(1);
        }

        // set up instance name and variables for running avg lookup response
        // time
        String instanceName = "Client" + PeerId;

        // checks for shared folder, if doesn't exist, create one
        String dirname = instanceName;
        File dir = new File(dirname);
        if (!dir.exists()) {
            System.out.println("Creating new shared directory");
            dir.mkdir();
        }

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
        Client ClientServe = null;
        ClientServe = new Client();
        Registry reg = null;
        try {
            reg = LocateRegistry.createRegistry(2002 + PeerId);
            System.out.println("Server is ready");

            // Naming.bind(instanceName, ClientServe);
            reg.rebind(instanceName, ClientServe);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            System.out.println("\nError - Peer "
                    + PeerId
                    + " is already bound.  Please choose a different peer id or restart rmiregistry\n");
            System.exit(0);
        }

        System.out.println("ClientServer running...PeerID = " + PeerId);

        // Call registry for p2pServer
        // register files in shared folder
        Server IndexServer = null;
        try {
            Registry reg2 = LocateRegistry.getRegistry("127.0.0.1", 2002);
            IndexServer = (Server) reg2.lookup("Server");
            register(dir, IndexServer);
        } catch (NotBoundException e) {
            // TODO Auto-generated catch block
            System.out.println("\nError - Index Server not bound.  Please start index server before launching client");
            System.out.println(e);
            reg.unbind(instanceName);
            System.exit(0);
        }

        // main UI loop
        int choice = 0;
        int peerServer = 0;
        String s;
        Scanner scan = new Scanner(System.in);
        InputStreamReader stream = new InputStreamReader(System.in);
        BufferedReader in = new BufferedReader(stream);
        boolean loop = true;
        String filename;
        while (loop) {
            System.out.println("\n\nPeerID: " + PeerId);
            System.out.println("Options:");
            System.out.println("1 - Search for filename");
            System.out.println("2 - Obtain filename from peer");
            System.out.println("3 - List files in shared directory");
            System.out.println("4 - Exit");
            System.out.print("\n\n:");

            s = scan.nextLine();
            try {
                choice = Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                System.out.println("\nPlease enter an integer\n");
            }

            switch (choice) {
                case 1:
                    System.out.print("Enter filename: ");
                    filename = in.readLine();
                    System.out.print("\n");
                    search(filename, IndexServer);
                    break;
                case 2:
                    System.out.print("Enter filename: ");
                    filename = in.readLine();
                    System.out.print("\nEnter peer: ");
                    try {
                        s = scan.nextLine();
                        peerServer = Integer.parseInt(s.trim());
                    } catch (NumberFormatException e) {
                        System.out.println("\nPlease enter an integer\n");
                    }
                    // Get response time also
                    final long downloadstartTime = System.nanoTime();
                    final long downloadendTime;
                    try {
                        getFile(filename, peerServer, instanceName, IndexServer);

                    } finally {
                        downloadendTime = System.nanoTime();
                    }
                    final long downloadduration = downloadendTime
                            - downloadstartTime;
                    System.out.println("Download Response time: "
                            + downloadduration + "ns");
                    break;

                case 3:
                    list(dir, IndexServer);
                    break;

                case 4:
                    if (numLookups > 0) {
                        avgResponseTime = aggregateResponseTime / numLookups;
                        System.out.println("\nAverage Lookup Response time for this session: "
                                + avgResponseTime + "\n");
                    } else {
                        System.out.println("Average Lookup Response time for this session: 0");
                    }
                    loop = false;
                    break;
                default:
                    System.out.println("\nPlease enter a number between 1 and 4\n");
                    break;
            }

        }
        reg.unbind(instanceName);
        System.exit(0);
    }


    /**
     * Method for clients to obtain files from other clients reads in local file
     * from with buffered reader returns byte array containing file contents to
     * caller
     *
     * @param file
     * @param peer
     * @param instanceName
     * @return
     * @throws java.io.IOException
     */
    @Override
    public byte[] obtain(String file, int peer, String instanceName)
            throws IOException {

        String peerServer = "Client" + peer;

        // create reader in order to read local file into byte array
        BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
        String pathfile = instanceName + "/" + file;

        // test if file exists
        File readfile = new File(pathfile);
        if (!readfile.exists()) {
            return null;
        }

        // File length
        int size = (int) readfile.length();
        if (size > Integer.MAX_VALUE) {
            System.out.println("File is to large");
        }
        byte[] bytes = new byte[size];
        DataInputStream dis = new DataInputStream(new FileInputStream(readfile));
        int read = 0;
        int numRead = 0;
        while (read < bytes.length
                && (numRead = dis.read(bytes, read, bytes.length - read)) >= 0) {
            read = read + numRead;
        }

        // Ensure all the bytes have been read in
        if (read < bytes.length) {
            System.out.println("Unable to read: " + readfile.getName());
        }
        return bytes;

    }

    /**
     * Method for clients to list files currently in their shared directory
     */
    private static void list(File dir, Server indexServer) {
        // lists files in shared directory
        File[] sharedfiles = dir.listFiles();

        System.out.println("\n\nFiles in shared directory: ");
        for (File sharedfile : sharedfiles) {
            System.out.println(sharedfile.getName());
        }
        System.out.print("\n\n");

    }

    /**
     * Searches index server for filename and returns list of peers sharing that
     * file
     */
    private static void search(String filename, Server IndexServer)
            throws RemoteException {

        numLookups++;
        // Get response time also
        final long lookupstartTime = System.nanoTime();
        final long lookupendTime;
        try {

            List<Integer> Peers = new ArrayList<>();
            Peers = IndexServer.search(filename);

            // No one sharing that file
            if (Peers == null) {
                System.out.println("\n\nNo peers appear to be sharing file (" + filename + ")\n\n");
                return;
            }

            // 1 or more peers has file
            Iterator<Integer> i = Peers.listIterator();
            System.out.print("The following peers have the file (" + filename + ") :\n");
            while (i.hasNext()) {
                System.out.print(i.next() + "\n");
            }
            System.out.print("\n\n");

        } finally {
            lookupendTime = System.nanoTime();
        }
        final long lookupduration = lookupendTime - lookupstartTime;
        System.out.println("Lookup Response time: " + lookupduration + " ns");
        aggregateResponseTime += lookupduration;

    }

    /**
     * Method to register shared files with Index Server Automatically called at
     * run time of client Subsequently called each time a new file is downloaded
     * into shared directory
     *
     */
    private static void register(File dir, Server IndexServer)
            throws RemoteException {

        // go through shared directory and register filenames with index server
        File[] sharedfiles = dir.listFiles();

        System.out.println("# of files registered: " + sharedfiles.length);

        // no files
        if (sharedfiles.length == 0) {
            return;
        }

        // register all files
        for (File sharedfile : sharedfiles) {
            IndexServer.registry(PeerId, sharedfile.getName());
        }
    }

    /**
     * Method to set up connection with peer, call obtain() to get file, write
     * file to local shared directory and register file with Index Server
     *
     */
    private static void getFile(String filename, int peerServer,
            String instanceName, Server IndexServer)
            throws FileNotFoundException, IOException {

        // Connect to peer and obtain file
        ClientServer servingPeer;

        // byte array that will contain file contents
        byte[] temp = null;

        // attempt to lookup clientserver
        try {

            Registry reg2 = LocateRegistry.getRegistry("127.0.0.1", 2002 + peerServer);
            servingPeer = (ClientServer) reg2.lookup("Client" + peerServer);
            //servingPeer = (ClientServer) Naming.lookup("rmi://localhost/" + "Client" + peerServer);
            temp = servingPeer.obtain(filename, peerServer, "Client" + peerServer);
        } catch (NotBoundException e) {
            // TODO Auto-generated catch block
            System.out.println("\nError - Invalid peer entered.  Please enter valid peer");
            return;
        }

        // invalid file
        if (temp == null) {
            System.out.println("\nError - Invalid filename for specified peer.  Please enter valid filename");
            return;
        }

        // write file to shared directory
        String strFilePath = instanceName + "/" + filename;

        try {
            FileOutputStream fos = new FileOutputStream(strFilePath);

            fos.write(temp);

            fos.close();

            // successful write add to registry
            IndexServer.registry(PeerId, filename);
            System.out.println("\ndisplay file " + filename);

        } catch (FileNotFoundException ex) {
            System.out.println("FileNotFoundException : " + ex);
        } catch (IOException ioe) {
            System.out.println("IOException : " + ioe);
        }
    }

}
