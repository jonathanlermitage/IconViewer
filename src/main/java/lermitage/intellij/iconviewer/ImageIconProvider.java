package lermitage.intellij.iconviewer;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.AsynchronousResourceLoader;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.parser.StaxSVGLoader;
import com.github.weisj.jsvg.util.ResourceUtil;
import com.intellij.ide.IconProvider;
import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.ImageUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
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

    private final ThreadLocal<Boolean> localContextUpdated = ThreadLocal.withInitial(() -> false);
    private final ThreadLocal<Boolean> isJSVGSevereLoggingDisabled = ThreadLocal.withInitial(() -> false);

    /**
     * Image formats supported by TwelveMonkeys.
     */
    private final ThreadLocal<Set<String>> extendedImgFormats = ThreadLocal.withInitial(Collections::emptySet);

    /**
     * Image formats supported when Android plugin is enabled.
     */
    private final Set<String> androidImgFormats = new HashSet<>(Arrays.asList("webm", "webp"));

    @Nullable
    public Icon getIcon(@NotNull PsiElement psiElement, int flags) {
        try {
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

        } catch (Exception e) {
            LOGGER.warn("Error loading preview Icon - " + psiElement.getContainingFile().getVirtualFile().getCanonicalPath(), e);
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
            LOGGER.warn("Error loading preview Icon - " + canonicalFile.getCanonicalPath(), e);
        }
        return null;
    }

    /**
     * Load graphics libraries (TwelveMonkeys) in order to make the JVM able to manipulate additional image formats.
     */
    private synchronized void enhanceImageIOCapabilities() {
        if (!localContextUpdated.get()) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                ImageIO.scanForPlugins();
            } finally {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
            localContextUpdated.set(true);
            extendedImgFormats.set(Stream.of(ImageIO.getReaderFormatNames()).map(String::toLowerCase).collect(Collectors.toSet()));
            extendedImgFormats.get().add("svg");
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("ImageIO plugins updated with TwelveMonkeys capabilities");
                LOGGER.debug("Image file formats supported by Twelvemonkeys library: " + getExtendedImgFormats());
            }
        }
    }

    /*private static ByteArrayInputStream canonicalPathToByteArrayInputStream(@NotNull String canonicalPath) throws IOException {
        File file = new File(canonicalPath);
        String contents = Files.readString(file.toPath(), Charset.defaultCharset());
        Matcher matcher = cssVarRe.matcher(contents);
        String replaced = matcher.replaceAll("currentColor");
        return new ByteArrayInputStream(replaced.getBytes());
    }*/

    // FIXME IDE freezes when rendering many SVG files in parallel. Workaround: use a synchronized method
    private static synchronized SVGDocument loadSVG(String canonicalPath) throws IOException {
        return new SVGLoader().load(new File(canonicalPath).toURI().toURL());
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
            if (fileExtension.endsWith("svg")) {
                // IMPORTANT check https://github.com/search?q=repo%3AweisJ%2Fjsvg%20SEVERE&type=code
                //  and find JSVG code which logs failures with SEVERE level, and disable them in order to avoid
                //  IDE error reports for invalid SVG files
                if (!isJSVGSevereLoggingDisabled.get()) {
                    Logger.getFactory().getLoggerInstance(AsynchronousResourceLoader.class.getName()).setLevel(LogLevel.OFF);
                    Logger.getFactory().getLoggerInstance(ResourceUtil.class.getName()).setLevel(LogLevel.OFF);
                    Logger.getFactory().getLoggerInstance(StaxSVGLoader.class.getName()).setLevel(LogLevel.OFF);
                    Logger.getFactory().getLoggerInstance(SVGLoader.class.getName()).setLevel(LogLevel.OFF);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Prevent JSVG from generating error report on invalid SVG files");
                    }
                    isJSVGSevereLoggingDisabled.set(true);
                }

                SVGDocument svgDocument = loadSVG(canonicalPath);
                if (svgDocument == null) {
                    return null;
                }
                FloatSize size = svgDocument.size();
                BufferedImage image = ImageUtil.createImage((int) size.width, (int) size.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                svgDocument.render(null, graphics);
                Image thumbnail = scaleImage(image);
                if (thumbnail != null) {
                    return IconUtil.createImageIcon(thumbnail);
                }
            } else {
                try (ImageInputStream input = ImageIO.createImageInputStream(new File(canonicalPath))) {
                    Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                    while (readers.hasNext()) {
                        ImageReader reader = readers.next();
                        try {
                            reader.setInput(input);
                            BufferedImage originalImage = reader.read(0);
                            Image thumbnail = scaleImage(originalImage);
                            if (thumbnail != null) {
                                return IconUtil.createImageIcon(thumbnail);
                            }
                        } finally {
                            reader.dispose();
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error loading preview Icon - " + canonicalFile.getCanonicalPath(), e);
        }
        return null;
    }

    private BufferedImage read(@NotNull VirtualFile canonicalFile) throws IOException {
        return ImageIO.read(new File(canonicalFile.getPath()));
    }

    private String getExtendedImgFormats() {
        return extendedImgFormats.get().toString();
    }

    private static Image scaleImage(Image image) {
        return ImageUtil.scaleImage(image, SCALING_SIZE, SCALING_SIZE);
    }
}
