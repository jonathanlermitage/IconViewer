package lermitage.intellij.iconviewer;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ImageLoader;
import com.intellij.util.RetinaImage;
import com.intellij.util.SVGLoader;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class CustomIconLoader {

    private static final Logger LOGGER = Logger.getInstance(CustomIconLoader.class);

    public static Image loadSVGFromVirtualFile(VirtualFile virtualFile) throws IllegalArgumentException {
        if (virtualFile != null && virtualFile.getExtension() != null) {
            Image image;
            byte[] fileContents;
            try {
                fileContents = virtualFile.contentsToByteArray();
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileContents);
                image = SVGLoader.load(byteArrayInputStream, 1.0f);
            } catch (IOException ex) {
                // File is probably empty or being edited: it can't be valid and we don't care about SVG errors.
                LOGGER.debug(ex);
                return null;
            }
            if (image == null) {
                // We don't care about SVG loading errors.
                // We log a message because we should not get a null image but an IOException.
                LOGGER.info("Could not load SVG image properly: " + virtualFile.getCanonicalPath());
                return null;
            }
            try {
                return scaleImage(image);
            } catch (Exception ex) {
                // We don't care about SVG loading errors.
                // Should never occur.
                LOGGER.info("Could not scale SVG image properly: " + virtualFile.getCanonicalPath(), ex);
            }
        }
        return null;
    }

    private static Image scaleImage(Image image) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width or height are unknown.");
        }

        if (width == 16 && height == 16) {
            return image;
        }

        if (width == 32 && height == 32) {
            return RetinaImage.createFrom(image);
        }

        float widthToScaleTo = 16f;
        boolean retina = false;

        if (width >= 32 || height >= 32) {
            widthToScaleTo = 32f;
            retina = true;
        }

        Image scaledImage = ImageLoader.scaleImage(image, widthToScaleTo / Math.max(width, height));
        if (retina) {
            return RetinaImage.createFrom(scaledImage);
        }
        return scaledImage;
    }
}
