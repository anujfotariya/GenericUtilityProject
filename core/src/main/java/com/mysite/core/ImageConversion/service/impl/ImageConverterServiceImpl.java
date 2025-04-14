//package com.mysite.core.ImageConversion.service.impl;
//
//import com.aspose.imaging.Image;
//import com.day.cq.dam.api.Asset;
//import com.day.cq.dam.api.AssetManager;
//import com.day.cq.dam.api.Rendition;
//import com.luciad.imageio.webp.WebPWriteParam;
//import com.mysite.core.GenericBlogPackage.service.ResourceHelper;
//import com.mysite.core.ImageConversion.service.ImageConverterService;
//import org.apache.sling.api.resource.Resource;
//import org.apache.sling.api.resource.ResourceResolver;
//import org.osgi.service.component.annotations.Component;
//import org.osgi.service.component.annotations.Reference;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import com.day.cq.dam.api.DamConstants;
//
//import javax.imageio.ImageIO;
//import javax.imageio.ImageWriteParam;
//import javax.imageio.ImageWriter;
//import javax.imageio.stream.ImageOutputStream;
//import javax.jcr.Node;
//import java.awt.image.BufferedImage;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.InputStream;
//import java.util.Calendar;
//import java.util.Iterator;
//
//@Component(service = ImageConverterService.class,immediate = true)
//public class ImageConverterServiceImpl implements ImageConverterService {
//
//    @Reference
//    ResourceHelper resourceHelper;
//    Logger logger= LoggerFactory.getLogger(this.getClass());
//
//
//    @Override
//    public void convertImagesInFolder(String folderPath) {
//        ResourceResolver resourceResolver=null;
//        AssetManager assetManager=null;
//        try{
//            resourceResolver=resourceHelper.getResourceResolver();
//            assetManager=resourceResolver.adaptTo(AssetManager.class);
//
//            Resource folderResource = resourceResolver.getResource(folderPath);
//
//            if (folderResource == null) {
//                throw new IllegalArgumentException("Folder path does not exist: " + folderPath);
//            }
//            Iterator<Resource> resources = folderResource.listChildren();
//            while (resources.hasNext()) {
//                Resource resource = resources.next();
//                Asset asset = resource.adaptTo(Asset.class);
//                if (asset != null && isImage(asset)) {
//                    convertImageToWebP(asset);
//                }
//            }
//        }
//        catch (Exception e)
//        {
//            logger.error("Exception in image conversion {}",e.getMessage());
//        }
//
//
//    }
//
//    private boolean isImage(Asset asset) {
//        String mimeType = asset.getMimeType();
//        return mimeType.startsWith("image/");
//    }
//
//    private void convertImageToWebP(Asset asset) throws Exception {
//        Rendition originalRendition = asset.getOriginal();
//        InputStream inputStream = originalRendition.adaptTo(InputStream.class);
//        BufferedImage image = ImageIO.read(inputStream);
//
//        int width = image.getWidth();
//        int height = image.getHeight();
//
//        // Create a ByteArrayOutputStream to hold the WebP image
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream);
//
//        // Find an appropriate WebP writer
//        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
//        if (!writers.hasNext()) {
//            throw new IllegalStateException("No WebP writer found");
//        }
//        ImageWriter writer = writers.next();
//
//        // Configure the writer
//        WebPWriteParam writeParam = new WebPWriteParam(writer.getLocale());
//        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//        writeParam.setCompressionQuality(1.0f); // Max quality
//
//        writer.setOutput(imageOutputStream);
//        writer.write(null, new javax.imageio.IIOImage(image, null, null), writeParam);
//        writer.dispose();
//        imageOutputStream.close();
//
//        // Convert the ByteArrayOutputStream to ByteArrayInputStream
//        ByteArrayInputStream webpStream = new ByteArrayInputStream(outputStream.toByteArray());
//
//        // Upload the WebP image back to DAM
//        String webpFileName = asset.getName().replaceFirst("[.][^.]+$", "") + ".webp";
//        Node parentNode = asset.adaptTo(Node.class);
//        Node webpNode = parentNode.addNode(webpFileName, "nt:file");
//        Node contentNode = webpNode.addNode("jcr:content", "nt:resource");
//        contentNode.setProperty("jcr:data", webpStream);
//        contentNode.setProperty("jcr:mimeType", "image/webp");
//        contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
//        parentNode.getSession().save();
//
//        System.out.println("Converted " + asset.getPath() + " to WebP with dimensions " + width + "x" + height);
//    }
//}
