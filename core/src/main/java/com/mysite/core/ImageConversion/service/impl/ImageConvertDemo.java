package com.mysite.core.ImageConversion.service.impl;

import com.luciad.imageio.webp.WebPWriteParam;
import com.mysite.core.ImageConversion.service.ImageConverterService;
import org.osgi.service.component.annotations.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

@Component(service = ImageConverterService.class,immediate = true)
public class ImageConvertDemo implements ImageConverterService {

    @Override
    public void convertImagesInFolder(String folderPath) throws IOException {
        String s =
                "https://upload.wikimedia.org/wikipedia/commons/f/f9/Phoenicopterus_ruber_in_S%C3%A3o_Paulo_Zoo.jpg";

        URL url = new URL(s.replaceAll(" ", "%20"));
        InputStream is = null;
        try {
            is = url.openStream();
        } catch (
                IOException e) {
            e.printStackTrace();
            return;
        }

        // Read the original image from the input stream
        BufferedImage originalImage = ImageIO.read(is);
        is.close(); // Close the stream after reading the image

        if (originalImage == null) {
            System.out.println("Failed to read the image.");
            return;
        }

        ImageOutputStream imgOutStrm = null;
        try {
            ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
            File outputFile = new File("D:\\output_image.webp");
            imgOutStrm = ImageIO.createImageOutputStream(outputFile);
            writer.setOutput(imgOutStrm);
            WebPWriteParam writeParam = new WebPWriteParam(writer.getLocale());
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionType(writeParam.getCompressionTypes()[WebPWriteParam.LOSSY_COMPRESSION]);
            writeParam.setCompressionQuality(0.4f); // Set compression quality
            writer.write(null, new IIOImage(originalImage, null, null), writeParam);
            System.out.println("Image saved as output_image.webp");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (imgOutStrm != null) {
                    imgOutStrm.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
