package lermitage.intellij.iconviewer;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IconUtil;
import com.intellij.util.ImageLoader;
import com.intellij.util.RetinaImage;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by David Sommer on 19.05.17.
 *
 * @author davidsommer
 * @author jonathanlermitage
 */
public class ImageIconProvider extends IconProvider {

    private static final Logger LOGGER = Logger.getInstance(ImageIconProvider.class);
    private static final int SCALING_SIZE = 16;
    private static final Pattern cssVarRe = Pattern.compile("var\\([-\\w]+\\)");

    private final ThreadLocal<Boolean> contextUpdated = ThreadLocal.withInitial(() -> false);

    /** Image formats supported by TwelveMonkeys. */
    private final ThreadLocal<Set<String>> extendedImgFormats = ThreadLocal.withInitial(Collections::emptySet);
    /** Image formats supported when Android plugin is enabled. */
    private final Set<String> androidImgFormats = new HashSet<>(Arrays.asList("webm", "webp"));

    @Nullable
    public Icon getIcon(@NotNull PsiElement psiElement, int flags) {
        PsiFile containingFile = psiElement.getContainingFile();
        if (containingFile != null
            && containingFile.getVirtualFile() != null
            && containingFile.getVirtualFile().getExtension() != null
            && containingFile.getVirtualFile().getCanonicalFile() != null
            && containingFile.getVirtualFile().getCanonicalFile().getCanonicalPath() != null
            && !containingFile.getVirtualFile().getCanonicalFile().getCanonicalPath().contains(".jar")) {

            VirtualFile canonicalFile = containingFile.getVirtualFile().getCanonicalFile();
            String fileExtension = containingFile.getVirtualFile().getExtension().toLowerCase();

            if (androidImgFormats.contains(fileExtension)) {
                return previewAndroidImage(canonicalFile);
            } else {
                return previewImageWithExtendedSupport(canonicalFile, fileExtension);
            }
        }
        return null;
    }

    @Nullable
    private Icon previewAndroidImage(@NotNull VirtualFile canonicalFile) {
        try {
            BufferedImage read = read(canonicalFile);
            if (read == null) {
                return null;
            }
            Image scaledInstance = read.getScaledInstance(SCALING_SIZE, SCALING_SIZE, BufferedImage.SCALE_SMOOTH);
            return scaledInstance == null ? null : new ImageIcon(scaledInstance);
        } catch (IOException e) {
            logWarn(e, canonicalFile);
        }
        return null;
    }

    /** Load graphics libraries (TwelveMonkeys) in order to make the JVM able to manipulate additional image formats. */
    private synchronized void enhanceImageIOCapabilities() {
        if (!contextUpdated.get()) {
            Thread.currentThread().setContextClassLoader(ImageIconProvider.class.getClassLoader());
            ImageIO.scanForPlugins();
            contextUpdated.set(true);
            extendedImgFormats.set(Stream.of(ImageIO.getReaderFormatNames()).map(String::toLowerCase).collect(Collectors.toSet()));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Image file formats supported by Twelvemonkeys library: " + getExtendedImgFormats());
            }
            LOGGER.info("ImageIO plugins updated with TwelveMonkeys capabilities");
        }
    }

    private static Object getFile(@NotNull String canonicalPath, boolean isSVG) {
        File file = new File(canonicalPath);
        if (isSVG) {
            try {
                String contents = FileUtils.readFileToString(file, Charset.defaultCharset());
                Matcher matcher = cssVarRe.matcher(contents);
                String replaced = matcher.replaceAll("currentColor");
                return new ByteArrayInputStream(replaced.getBytes());
            } catch (Exception e) {
                LOGGER.debug("Failed to read " + canonicalPath, e);
            }
        }
        return file;
    }

    @Nullable
    private Icon previewImageWithExtendedSupport(@NotNull VirtualFile canonicalFile, @NotNull String fileExtension) {
        try {
            enhanceImageIOCapabilities();

            if (!extendedImgFormats.get().contains(fileExtension)) {
                return null;
            }
            String canonicalPath = canonicalFile.getCanonicalPath();
            if (canonicalPath == null) {
                return null;
            }
            boolean isSVG = fileExtension.endsWith("svg");
            try (ImageInputStream input = ImageIO.createImageInputStream(getFile(canonicalPath, isSVG))) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                while (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(input);
                        BufferedImage originalImage = reader.read(0);
                        Image thumbnail = scaleImage(originalImage, isSVG);
                        if (thumbnail != null) {
                            return IconUtil.createImageIcon(thumbnail);
                        }
                    } finally {
                        reader.dispose();
                    }
                }
            }
        } catch (Exception e) {
            logWarn(e, canonicalFile);
        }
        return null;
    }

    private BufferedImage read(@NotNull VirtualFile canonicalFile) throws IOException {
        return ImageIO.read(new File(canonicalFile.getPath()));
    }

    private void logWarn(Exception e, @NotNull VirtualFile virtualFile) {
        LOGGER.warn("Error loading preview Icon - " + virtualFile.getCanonicalPath(), e);
    }

    private String getExtendedImgFormats() {
        return extendedImgFormats.get().toString();
    }

    private static Image scaleImage(Image image, boolean isSVG) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);

        if (isSVG) { // generate high-quality thumbnail
            Image scaledImage = ImageLoader.scaleImage(image, 128, 128);
            return RetinaImage.createFrom(scaledImage, 8f, null);
        }

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
