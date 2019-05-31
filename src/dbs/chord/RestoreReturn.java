package dbs.chord;

import dbs.filesystem.threads.ResultCode;

public class RestoreReturn{

    private ResultCode resultCode;
    private byte[] fileContent;

    public RestoreReturn(ResultCode resultCode, byte[] fileContent) {
        this.resultCode = resultCode;
        this.fileContent = fileContent;
    }

    public ResultCode getResultCode(){
        return resultCode;
    }

    public byte[] getFileContent(){
        return fileContent;
    }

}