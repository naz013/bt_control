package com.example.helio.arduino.multimeter;

class FileItem {
    private String fileName;
    private String fullPath;

    FileItem(String fileName, String fullPath) {
        this.fileName = fileName;
        this.fullPath = fullPath;
    }

    String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }
}
