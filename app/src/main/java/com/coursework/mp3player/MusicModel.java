package com.coursework.mp3player;

public class MusicModel {
    private String Title;
    private String Artist;
    private String filePath;
    public String getArtist() {
        return Artist;
    }

    public void setArtist(String artist) {
        Artist = artist;
    }

    public MusicModel(String title,String artist,String filePath){
        this.Title=title;
        this.Artist = artist;
        this.filePath=filePath;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }
    public String getFilePath(){
        return this.filePath;
    }
    public void setFilePath(String path){
        this.filePath=path;
    }
}
