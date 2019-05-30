package dbs.chord;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;

import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.LookupMessage;
import dbs.chord.messages.ResponsibleMessage;

public final class ChordLogger {

    private static final boolean PRINT_IN = true;
    private static final boolean PRINT_OUT = true;

    private static final boolean PRINT_NODE_IMPORTANT = true;
    private static final boolean PRINT_NODE_TRACK = false;
    private static final boolean PRINT_SOCKET_TRACK = false;

    private static final boolean PRINT_INTERNAL_ERROR = true;
    private static final boolean PRINT_EXTERNAL_ERROR = true;
    private static final boolean PRINT_NODE_ERROR = true;
    private static final boolean PRINT_IO_ERROR = true;
    private static final boolean PRINT_SOCKET_ERROR = true;
    private static final boolean PRINT_DROPS = true;

    private static final boolean PRINT_NODE_STABILIZE = true;
    private static final boolean PRINT_NODE_FIX_FINGERS = true;
    private static final boolean PRINT_NODE_CHECK_PREDECESSOR = true;

    private static boolean USE_COLORS = false, USE_PREFIX = true;

    private static void format(int code, String msg, boolean prefix) {
        if (USE_COLORS && (USE_PREFIX || prefix)) {
            System.out.print(String.format("\u001B[%sm%s %s.\u001B[0m\n", colorMap[code], prefixMap[code], msg));
        } else if (USE_COLORS) {
            System.out.print(String.format("\u001B[%sm%s.\u001B[0m\n", colorMap[code], msg));
        } else if ((USE_PREFIX || prefix)) {
            System.out.print(String.format("%s %s.\n", prefixMap[code], msg));
        } else {
            System.out.print(String.format("%s.\n", msg));
        }
    }

    private static void format(int code, String msg) {
        format(code, msg, false);
    }

    public static void logIn(ChordMessage message) {
        if (PRINT_IN && inSet.contains(message.getClass().getName())) {
            String node = message.getSender().shortStr();
            format(INPUT, message + " from " + node);
        }
    }

    public static void logOut(ChordMessage message, NodeInfo destination) {
        if (PRINT_OUT && outSet.contains(message.getClass().getName())) {
            String node = destination.shortStr();
            format(OUTPUT, message + " to " + node);
        }
    }

    public static void logNodeImportant(String msg) {
        if (PRINT_NODE_IMPORTANT) {
            format(IMPORTANT, msg);
        }
    }

    public static void logNode(String msg) {
        if (PRINT_NODE_TRACK) {
            format(LOGINFO, msg);
        }
    }

    public static void logSocket(String msg) {
        if (PRINT_SOCKET_TRACK) {
            format(NEUTRAL, msg);
        }
    }

    public static void internal(String msg) {
        if (PRINT_INTERNAL_ERROR) {
            format(SEVERE, "Internal: " + msg);
        }
    }

    public static void external(String msg) {
        if (PRINT_EXTERNAL_ERROR) {
            format(WARNING, "External: " + msg);
        }
    }

    public static void nodeError(String msg) {
        if (PRINT_NODE_ERROR) {
            format(ERROR, "Node: " + msg);
        }
    }

    public static void ioError(IOException exception) {
        if (PRINT_IO_ERROR) {
            format(ERROR, "IO: " + exception.getMessage());
        }
    }

    public static void socketError(IOException exception) {
        if (PRINT_SOCKET_ERROR) {
            format(ERROR, "Socket: " + exception.getMessage());
        }
    }

    public static void dropped(ChordMessage message, String reason) {
        if (PRINT_DROPS) {
            format(DROP, "Dropped " + message + " from " + message.getSender().shortStr() + ": " + reason);
        }
    }

    public static void logStabilize(String msg) {
        if (PRINT_NODE_STABILIZE) {
            format(STABILIZE, msg, true);
        }
    }

    public static void logFixFingers(int i, String msg) {
        if (PRINT_NODE_FIX_FINGERS) {
            BigInteger fingerMin = Chord.ithFinger(Node.get().getSelf().getChordId(), i);
            String text = String.format("finger[%d %s]: %s", i, Chord.percentStr(fingerMin), msg);
            format(FIXFINGERS, text, true);
        }
    }

    public static void logCheckPredecessor(String msg) {
        if (PRINT_NODE_CHECK_PREDECESSOR) {
            format(CHECK_PREDECESSOR, msg, true);
        }
    }

    private static final HashSet<String> inSet = new HashSet<>();
    private static final HashSet<String> outSet = new HashSet<>();
    private static final String[] colorMap = new String[20];
    private static final String[] prefixMap = new String[20];

    private static final int IMPORTANT = 0;
    private static final int SEVERE = 1;
    private static final int ERROR = 2;
    private static final int WARNING = 3;
    private static final int NEUTRAL = 4;
    private static final int LOGINFO = 5;
    private static final int INPUT = 6;
    private static final int OUTPUT = 7;
    private static final int SUBPROTOCOL = 8;
    private static final int STABILIZE = 9;
    private static final int FIXFINGERS = 10;
    private static final int CHECK_PREDECESSOR = 11;
    private static final int DROP = 12;

    static {
        inSet.add(LookupMessage.class.getName());
        inSet.add(ResponsibleMessage.class.getName());
        //inSet.add(StabilizeMessage.class.getName());
        //inSet.add(PredecessorMessage.class.getName());
        outSet.add(LookupMessage.class.getName());
        outSet.add(ResponsibleMessage.class.getName());
        //outSet.add(StabilizeMessage.class.getName());
        //outSet.add(PredecessorMessage.class.getName());

        colorMap[IMPORTANT] = "1;36";
        colorMap[SEVERE] = "1;31";
        colorMap[ERROR] = "31";
        colorMap[WARNING] = "33";
        colorMap[NEUTRAL] = "37";
        colorMap[LOGINFO] = "36";
        colorMap[INPUT] = "35";
        colorMap[OUTPUT] = "32";
        colorMap[SUBPROTOCOL] = "34";
        colorMap[STABILIZE] = "34";
        colorMap[FIXFINGERS] = "34";
        colorMap[CHECK_PREDECESSOR] = "34";
        colorMap[DROP] = "37";

        prefixMap[IMPORTANT] = "[IMPORTANT]";
        prefixMap[SEVERE] = "[SEVERE] ";
        prefixMap[ERROR] = "[ERROR]  ";
        prefixMap[WARNING] = "[WARNING]";
        prefixMap[NEUTRAL] = "[NEUTRAL]";
        prefixMap[LOGINFO] = "[LOG]";
        prefixMap[INPUT] = "[IN] ";
        prefixMap[OUTPUT] = "[OUT]";
        prefixMap[SUBPROTOCOL] = "[SUBPROTOCOL]";
        prefixMap[STABILIZE] = "[STABILIZE]";
        prefixMap[FIXFINGERS] = "[FIXFINGERS]";
        prefixMap[CHECK_PREDECESSOR] = "[CHECK_PREDECESSOR]";
        prefixMap[DROP] = "[DROP]";
    }
}
