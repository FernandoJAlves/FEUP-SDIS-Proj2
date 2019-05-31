package dbs.filesystem.threads;

public enum ResultCode {

    OK(0), NOT_RESPONSIBLE(0x10), NO_OVERWRITE(0x20), NOT_FOUND(0x40), INTERNAL_ERROR(0x50);

    public int code;

    ResultCode(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        switch (this) {
        case OK:
            return "OK";
        case NOT_RESPONSIBLE:
            return "NOT_RESPONSIBLE";
        case NO_OVERWRITE:
            return "NO_OVERWRITE";
        case NOT_FOUND:
            return "NOT_FOUND";
        case INTERNAL_ERROR:
            return "INTERNAL_ERROR";
        }
    }
}
