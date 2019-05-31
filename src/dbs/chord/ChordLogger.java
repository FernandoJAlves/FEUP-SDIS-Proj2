package dbs.chord;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;

import dbs.chord.messages.ChordMessage;
import dbs.chord.messages.LookupMessage;
import dbs.chord.messages.ResponsibleMessage;
import dbs.chord.messages.protocol.BackupMessage;
import dbs.chord.messages.protocol.BackupResponseMessage;

public final class ChordLogger {

    private static final boolean PRINT_IN = true;
    private static final boolean PRINT_OUT = true;
    private static final boolean PRINT_LOOKUPS = true;
    private static final boolean PRINT_BACKUP = true;
    private static final boolean PRINT_RESTORE = true;
    private static final boolean PRINT_DELETE = true;

    private static final boolean PRINT_NODE_IMPORTANT = true;
    private static final boolean PRINT_NODE_TRACK = false;
    private static final boolean PRINT_SOCKET_TRACK = false;

    private static final boolean PRINT_INTERNAL_ERROR = true;
    private static final boolean PRINT_EXTERNAL_ERROR = true;
    private static final boolean PRINT_BAD_PROGRESS = true;
    private static final boolean PRINT_IO_ERROR = true;
    private static final boolean PRINT_SOCKET_ERROR = true;
    private static final boolean PRINT_DROPS = true;

    private static final boolean PRINT_NODE_STABILIZE = false;
    private static final boolean PRINT_NODE_FIX_FINGERS = false;
    private static final boolean PRINT_NODE_CHECK_PREDECESSOR = false;
    private static final boolean PRINT_NODE_JOIN = true;

    public static boolean DUMP_NODE_TABLE = true; // required for extract.sh
    public static boolean USE_COLORS = true, USE_PREFIX = true;

    private static final HashSet<String> inSet = new HashSet<>();
    private static final HashSet<String> outSet = new HashSet<>();

    // Decide what messages get reported. This requires PRINT_IN / PRINT_OUT to be set to true.
    static {
        inSet.add(LookupMessage.class.getName());
        inSet.add(ResponsibleMessage.class.getName());
        inSet.add(BackupMessage.class.getName());
        inSet.add(BackupResponseMessage.class.getName());
        //inSet.add(StabilizeMessage.class.getName());
        //inSet.add(PredecessorMessage.class.getName());

        outSet.add(LookupMessage.class.getName());
        outSet.add(ResponsibleMessage.class.getName());
        outSet.add(BackupMessage.class.getName());
        outSet.add(BackupResponseMessage.class.getName());
        //outSet.add(StabilizeMessage.class.getName());
        //outSet.add(PredecessorMessage.class.getName());
    }

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

    // Messages received
    public static void logIn(ChordMessage message) {
        if (PRINT_IN && inSet.contains(message.getClass().getName())) {
            String node = message.getSender().shortStr();
            format(INPUT, message + " from " + node);
        }
    }

    // Messages sent
    public static void logOut(ChordMessage message, NodeInfo destination) {
        if (PRINT_OUT && outSet.contains(message.getClass().getName())) {
            String node = destination.shortStr();
            format(OUTPUT, message + " to " + node);
        }
    }

    // Lookup API requests
    public static void logLookup(BigInteger chordId, String message) {
        if (PRINT_LOOKUPS) {
            format(LOOKUP, "lookup(" + Chord.percentStr(chordId) + "): " + message);
        }
    }

    // Important node metadata, such as new successor and predecessor.
    public static void logNodeImportant(String msg) {
        if (PRINT_NODE_IMPORTANT) {
            format(IMPORTANT, msg);
        }
    }

    // Node tracking, unimportant information.
    public static void logNode(String msg) {
        if (PRINT_NODE_TRACK) {
            format(LOGINFO, msg);
        }
    }

    // Socket tracking, unimportant information.
    public static void logSocket(String msg) {
        if (PRINT_SOCKET_TRACK) {
            format(NEUTRAL, msg);
        }
    }

    // Internal IO errors found, such as no observers for a received message and immediate loopback messages.
    public static void internal(String msg) {
        if (PRINT_INTERNAL_ERROR) {
            format(SEVERE, "Internal: " + msg);
        }
    }

    // External IO errors found, where another Node did not comply with the protocol.
    public static void external(String msg) {
        if (PRINT_EXTERNAL_ERROR) {
            format(WARNING, "External: " + msg);
        }
    }

    // Disrupted progress errors, such as not being able to update successor, predecessor or joining Chord network.
    public static void progress(String msg) {
        if (PRINT_BAD_PROGRESS) {
            format(ERROR, "Node: " + msg);
        }
    }

    // Unexpected IOException when reading or writing a message.
    public static void ioError(IOException exception) {
        if (PRINT_IO_ERROR) {
            format(ERROR, "IO: " + exception.getMessage());
        }
    }

    // Unexpected SocketException when reading or writing a message, or opening a socket.
    public static void socketError(IOException exception) {
        if (PRINT_SOCKET_ERROR) {
            format(ERROR, "Socket: " + exception.getMessage());
        }
    }

    // Valid messages dropped by this Node due to unstable Chord network state.
    public static void dropped(ChordMessage message, String reason) {
        if (PRINT_DROPS) {
            format(DROP, "Dropped " + message + " from " + message.getSender().shortStr() + ": " + reason);
        }
    }

    // Stabilize subprotocol messages
    public static void logStabilize(String msg) {
        if (PRINT_NODE_STABILIZE) {
            format(STABILIZE, msg, true);
        }
    }

    // FixFingers subprotocol messages
    public static void logFixFingers(int i, String msg) {
        if (PRINT_NODE_FIX_FINGERS) {
            BigInteger fingerMin = Chord.ithFinger(Node.get().getSelf().getChordId(), i);
            String text = String.format("finger[%d %s]: %s", i, Chord.percentStr(fingerMin), msg);
            format(FIXFINGERS, text, true);
        }
    }

    // CheckPredecessor subprotocol messages
    public static void logCheckPredecessor(String msg) {
        if (PRINT_NODE_CHECK_PREDECESSOR) {
            format(CHECK_PREDECESSOR, msg, true);
        }
    }

    // Join subprotocol messages
    public static void logJoin(String msg) {
        if (PRINT_NODE_JOIN) {
            format(JOIN, msg, true);
        }
    }

    // Backup subprotocol messages
    public static void logBackup(String msg) {
        if (PRINT_BACKUP) {
            format(BACKUP, msg, true);
        }
    }

    // Backup subprotocol messages
    public static void logBackup(String filename, String msg) {
        if (PRINT_BACKUP) {
            format(BACKUP, "{" + filename + "} " + msg, true);
        }
    }

    // Restore subprotocol messages
    public static void logRestore(String msg) {
        if (PRINT_RESTORE) {
            format(RESTORE, msg, true);
        }
    }

    // Restore subprotocol messages
    public static void logRestore(String filename, String msg) {
        if (PRINT_RESTORE) {
            format(RESTORE, "{" + filename + "} " + msg, true);
        }
    }

    // Delete subprotocol messages
    public static void logDelete(String msg) {
        if (PRINT_DELETE) {
            format(DELETE, msg, true);
        }
    }

    // Delete subprotocol messages
    public static void logDelete(String filename, String msg) {
        if (PRINT_DELETE) {
            format(DELETE, "{" + filename + "} " + msg, true);
        }
    }

    // Severe messages
    public static void logSevere(String msg) {
        format(SEVERE, msg, true);
    }

    // * Internals

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
    private static final int JOIN = 12;
    private static final int DROP = 13;
    private static final int LOOKUP = 14;
    private static final int BACKUP = 15;
    private static final int RESTORE = 16;
    private static final int DELETE = 17;

    static {
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
        colorMap[JOIN] = "1;34";
        colorMap[DROP] = "37";
        colorMap[LOOKUP] = "36";
        colorMap[BACKUP] = "1;35";
        colorMap[RESTORE] = "1;36";
        colorMap[DELETE] = "1;34";

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
        prefixMap[JOIN] = "[JOIN]";
        prefixMap[DROP] = "[DROP]";
        prefixMap[LOOKUP] = "[LOOKUP]";
        prefixMap[BACKUP] = "[BACKUP]";
        prefixMap[RESTORE] = "[RESTORE]";
        prefixMap[DELETE] = "[DELETE]";
    }
}
