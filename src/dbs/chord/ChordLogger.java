package dbs.chord;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;

import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.LookupMessage;
import dbs.chord.messages.ResponsibleMessage;

public class ChordLogger {

    private static final boolean PRINT_IN = true;
    private static final boolean PRINT_OUT = true;
    private static final boolean PRINT_NODE_TRACK = true;
    private static final boolean PRINT_SOCKET_TRACK = true;
    private static final boolean PRINT_INTERNAL_ERROR = true;
    private static final boolean PRINT_EXTERNAL_ERROR = true;
    private static final boolean PRINT_NODE_ERROR = true;
    private static final boolean PRINT_IO_ERROR = true;
    private static final boolean PRINT_SOCKET_ERROR = true;

    private static final boolean PRINT_NODE_STABILIZE = false;
    private static final boolean PRINT_NODE_FIX_FINGERS = true;
    private static final boolean PRINT_NODE_CHECK_PREDECESSOR = false;

    private static final HashSet<String> inSet = new HashSet<>();
    private static final HashSet<String> outSet = new HashSet<>();

    static {
        inSet.add(LookupMessage.class.getName());
        inSet.add(ResponsibleMessage.class.getName());
        //inSet.add(StabilizeMessage.class.getName());
        //inSet.add(PredecessorMessage.class.getName());
        outSet.add(LookupMessage.class.getName());
        outSet.add(ResponsibleMessage.class.getName());
        //outSet.add(StabilizeMessage.class.getName());
        //outSet.add(PredecessorMessage.class.getName());
    }

    public static final void logIn(ChordMessage message) {
        if (PRINT_IN && inSet.contains(message.getClass().getName())) {
            String node = message.getSender().shortStr();
            System.out.print("\u001B[35mIN " + message + " from " + node + ".\u001B[0m\n");
        }
    }

    public static final void logOut(ChordMessage message, NodeInfo destination) {
        if (PRINT_OUT && outSet.contains(message.getClass().getName())) {
            String node = destination.shortStr();
            System.out.print("\u001B[32mOUT " + message + " to " + node + ".\u001B[0m\n");
        }
    }

    public static final void logNode(String message) {
        if (PRINT_NODE_TRACK) {
            System.out.print("\u001B[36m" + message + ".\u001B[0m\n");
        }
    }

    public static final void logSocket(String message) {
        if (PRINT_SOCKET_TRACK) {
            System.out.print("\u001B[37m" + message + ".\u001B[0m\n");
        }
    }

    public static final void internal(String message) {
        if (PRINT_INTERNAL_ERROR) {
            System.err.print("\u001B[31mINTERNAL ERROR: " + message + ".\u001B[0m\n");
        }
    }

    public static final void external(String message) {
        if (PRINT_EXTERNAL_ERROR) {
            System.err.print("\u001B[33mEXTERNAL ERROR: " + message + ".\u001B[0m\n");
        }
    }

    public static final void nodeError(String message) {
        if (PRINT_NODE_ERROR) {
            System.err.print("\u001B[31mNODE ERROR: " + message + ".\u001B[0m\n");
        }
    }

    public static final void ioError(IOException exception) {
        if (PRINT_IO_ERROR) {
            System.err.print("\u001B[31mIO ERROR: " + exception.getMessage() + ".\u001B[0m\n");
            exception.printStackTrace(System.err);
        }
    }

    public static final void socketError(IOException exception) {
        if (PRINT_SOCKET_ERROR) {
            System.err.print("\u001B[31mSOCKET ERROR: " + exception.getMessage() + ".\u001B[0m\n");
            exception.printStackTrace(System.err);
        }
    }

    public static final void logStabilize(String message) {
        if (PRINT_NODE_STABILIZE) {
            System.out.print("\u001B[34mStabilize: " + message + ".\u001B[0m\n");
        }
    }

    public static final void logFixFingers(int i, BigInteger finger, String message) {
        if (PRINT_NODE_FIX_FINGERS) {
            String percent = Chord.percentStr(finger);
            System.out.print("\u001B[34mFix Fingers[" + i + " " + percent + "]: " + message + ".\u001B[0m\n");
        }
    }

    public static final void logCheckPredecessor(String message) {
        if (PRINT_NODE_CHECK_PREDECESSOR) {
            System.out.print("\u001B[34mCheckPredecessor:" + message + ".\u001B[0m\n");
        }
    }
}
