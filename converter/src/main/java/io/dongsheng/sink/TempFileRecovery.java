package io.dongsheng.sink;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public class TempFileRecovery {
    private String fileNamePrefix;
    private Integer fileNum;
    private List<Long> offsets;
    @JsonIgnore
    private String fileName;

    public TempFileRecovery() {
    }

    public TempFileRecovery(String fileNamePrefix, Integer fileNum, List<Long> offsets) {
        this.fileNamePrefix = fileNamePrefix;
        this.fileNum = fileNum;
        this.offsets = offsets;
        this.fileName = fileNamePrefix + '-' + fileNum + ".recovery";
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

    public List<Long> getOffsets() {
        return offsets;
    }

    public void setOffsets(List<Long> offsets) {
        this.offsets = offsets;
    }

    public String getFileName() {
        return fileName;
    }
}
