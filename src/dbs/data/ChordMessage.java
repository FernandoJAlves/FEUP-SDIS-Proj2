package dbs.data;

import java.io.Serializable;

public abstract class ChordMessage implements Serializable {

    private static final long serialVersionUID = 315890078473063473L;

    private final String kind;

    public ChordMessage(String kind) {
        this.kind = kind;
    }

    public String getKind() {
        return kind;
    }
}
