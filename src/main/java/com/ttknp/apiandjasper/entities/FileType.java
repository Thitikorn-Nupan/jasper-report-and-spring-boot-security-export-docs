package com.ttknp.apiandjasper.entities;

public class FileType {
    
    private String fileExtension;

    public FileType() {
    }

    public FileType(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }


}
