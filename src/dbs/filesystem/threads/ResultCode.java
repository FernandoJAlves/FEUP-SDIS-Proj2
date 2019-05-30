package dbs.filesystem.threads;

public enum ResultCode {

    OK(0), NOT_RESPONSIBLE(0x10), NO_OVERWRITE(0x20), NOT_FOUND(0x40), INTERNAL_ERROR(0x50);

    private int code;

    ResultCode(int code) {
        this.code = code;
    }
}
