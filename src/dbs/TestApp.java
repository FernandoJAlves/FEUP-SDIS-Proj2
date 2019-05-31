package dbs;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {
    public TestApp() {
    }

    public enum ProtocolList {
        BACKUP, RESTORE, DELETE, RECLAIM, STATE
    }

    public static boolean containsProtocol(String test) {

        for (ProtocolList m : ProtocolList.values()) {
            if (m.name().equals(test)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNumber(String n) { 
        try {  
          Double.parseDouble(n);
          return true;
        } catch(NumberFormatException e){  
          return false;  
        }  
      }

    public static void main(String args[]) {

        if (args.length < 2 || args.length > 4) {
            System.out.println(
                    "ERROR: Usage should be 'java TestApp <remote_object_name> <sub_protocol> [<oper_1> <oper_2>]'");
            return;
        }

        try {
            String remotePeer = args[0];
            String protocol = args[1];

            if (containsProtocol(protocol)) {

                // Only create stub if protocol is valid
                Registry registry = LocateRegistry.getRegistry("localhost");
                RemoteInterface stub = (RemoteInterface) registry.lookup(remotePeer);

                switch (protocol) {
                case "BACKUP": {
                    if (args.length != 4) {
                        System.out.println("ERROR: Incorrect number of arguments in BACKUP");
                        return;
                    }
                    String filePath = args[2];
                    int repDeg;
                    if(isNumber(args[3])){
                        repDeg = Integer.parseInt(args[3]); // replication degree
                    }
                    else{
                        System.out.println("ERROR: Expected a number as <oper_2> of BACKUP");
                        return;
                    }                    
                    stub.backup(filePath, repDeg);
                    System.out.println("BACKUP!");
                    break;
                }
                case "RESTORE": {
                    if (args.length != 3) {
                        System.out.println("ERROR: Incorrect number of arguments in RESTORE");
                        return;
                    }
                    String filePath = args[2];
                    stub.restore(filePath);
                    System.out.println("RESTORE!");
                    break;
                }
                case "DELETE": {
                    if (args.length != 3) {
                        System.out.println("ERROR: Incorrect number of arguments in DELETE");
                        return;
                    }
                    String pathname = args[2];
                    stub.delete(pathname);
                    System.out.println("DELETE!");
                    break;
                }
                case "RECLAIM": {
                    if (args.length != 3) {
                        System.out.println("ERROR: Incorrect number of arguments in RECLAIM");
                        return;
                    }

                    int maxDiskSpace;
                    if(isNumber(args[2])){
                        maxDiskSpace = Integer.parseInt(args[2]); // replication degree
                    }
                    else{
                        System.out.println("ERROR: Expected a number as <oper_1> of RECLAIM");
                        return;
                    }
                    stub.reclaim(maxDiskSpace);
                    System.out.println("RECLAIM!");
                    break;
                }
                case "STATE": {
                    if (args.length != 2) {
                        System.out.println("ERROR: Incorrect number of arguments in STATE");
                        return;
                    }
                    System.out.println(stub.state());
                    System.out.println("STATE!");
                    break;
                }
                
                default:
                    System.out.println("ERROR: Protocol Switch Entered Default");
                    break;
                }
            } else
                System.out.println("ERROR: Invalid Protocol");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}