package io.dongsheng.sink;

public class TempFileMeta {
    private String fileNamePrefix;
    private Integer fileNum;
    private Integer recordCount;//start from 0
    private String metaFileName;

    public TempFileMeta() {
        this.fileNum = 0;
        this.recordCount = 0;
    }

    public TempFileMeta(String fileNamePrefix, Integer fileNum, Integer recordCount) {
        this.fileNamePrefix = fileNamePrefix;
        this.fileNum = fileNum;
        this.recordCount = recordCount;
        this.metaFileName = metaFileName(fileNamePrefix);
    }

    public String getFileNamePrefix() {
        return fileNamePrefix;
    }

    public void setFileNamePrefix(String fileNamePrefix) {
        this.fileNamePrefix = fileNamePrefix;
    }

    public Integer getFileNum() {
        return fileNum;
    }

    public void setFileNum(Integer fileNum) {
        this.fileNum = fileNum;
    }

    public Integer getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(Integer recordCount) {
        this.recordCount = recordCount;
    }

    public String getMetaFileName() {
        return metaFileName;
    }

    public void incFileNum() {
        this.fileNum = this.fileNum + 1;
    }

    public static String metaFileName(String fileNamePrefix) {
        return fileNamePrefix + ".meta";
    }
}
