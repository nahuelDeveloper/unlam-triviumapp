package com.trivium.cripto.triviumapp;

import java.io.IOException;

public interface Encrypter {
    public void initialize() throws IOException;
    public byte [] encrypt (Image image);
    public byte [] decrypt (Image image);
}
