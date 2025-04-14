package com.mysite.core.ImageConversion.service;

import java.io.IOException;

public interface ImageConverterService {
    void convertImagesInFolder(String folderPath) throws IOException;
}
