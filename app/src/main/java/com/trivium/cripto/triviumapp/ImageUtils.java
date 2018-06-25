package com.trivium.cripto.triviumapp;

import java.io.*;

public class ImageUtils {

    public static Boolean isJPEGImage(String filename) throws Exception {
        DataInputStream ins = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
        try {
            if (ins.readInt() == 0xffd8ffe0) {
                return true;
            } else {
                return false;
            }
        } finally {
            ins.close();
        }
    }
}
