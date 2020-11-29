package lermitage.intellij.iconviewer;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ImageLoader;
import com.intellij.util.RetinaImage;
import com.intellij.util.SVGLoader;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class CustomIconLoader {

    public static Image loadSVGFromVirtualFile(VirtualFile virtualFile) throws IllegalArgumentException {
        if (virtualFile.getExtension() != null) {
            Image image;
            byte[] fileContents;
            try {
                fileContents = virtualFile.contentsToByteArray();
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileContents);
                image = SVGLoader.load(byteArrayInputStream, 1.0f);
            } catch (IOException ex) {
                throw new IllegalArgumentException("IOException while trying to load SVG image.");
            }
            if (image == null) {
                throw new IllegalArgumentException("Could not load SVG image properly.");
            }
            return scaleImage(image);
        }
        return null;
    }

    private static Image scaleImage(Image image) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);

        if (width != height) {
            throw new IllegalArgumentException("Image should be square.");
        }

        if (width <= 0) {
            throw new IllegalArgumentException("Width and height are unknown.");
        }

        if (width == 16) {
            return image;
        }

        if (width == 32) {
            return RetinaImage.createFrom(image);
        }

        float widthToScaleTo = 16f;
        boolean retina = false;

        if (width >= 32) {
            widthToScaleTo = 32f;
            retina = true;
        }

        Image scaledImage = ImageLoader.scaleImage(image, widthToScaleTo / width);

        if (retina) {
            return RetinaImage.createFrom(scaledImage);
        }

        return scaledImage;
    }
}
