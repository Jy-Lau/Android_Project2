package com.coursework.mp3player;

import java.io.File;
import java.io.FileFilter;

public class AudioFilter implements FileFilter {
    private String[] extension = {".aac", ".mp3", ".wav", ".ogg", ".midi", ".3gp", ".mp4", ".m4a", ".amr", ".flac"};
    @Override
    public boolean accept(File pathname) {
        // loops through and determines the extension of all files in the directory
        // returns TRUE to only show the audio files defined in the String[] extension array
        for (String ext : extension) {
            if (pathname.getName().toLowerCase().endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
