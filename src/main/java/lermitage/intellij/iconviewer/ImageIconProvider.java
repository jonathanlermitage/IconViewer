package lermitage.intellij.iconviewer;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.intellij.ide.IconProvider;
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
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Graphics2D;
import java.awt.Image;
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
            logDebug(e, canonicalFile);
        }
        return null;
    }

    /**
     * Load graphics libraries (TwelveMonkeys) in order to make the JVM able to manipulate additional image formats.
     */
    private synchronized void enhanceImageIOCapabilities() {
        if (!localContextUpdated.get()) {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            ImageIO.scanForPlugins();
            localContextUpdated.set(true);
            extendedImgFormats.set(Stream.of(ImageIO.getReaderFormatNames()).map(String::toLowerCase).collect(Collectors.toSet()));
            extendedImgFormats.get().add("svg");
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Image file formats supported by Twelvemonkeys library: " + getExtendedImgFormats());
            }
            LOGGER.info("ImageIO plugins updated with TwelveMonkeys capabilities");
        }
    }

    private static ByteArrayInputStream canonicalPathToByteArrayInputStream(@NotNull String canonicalPath) throws IOException {
        File file = new File(canonicalPath);
        String contents = Files.readString(file.toPath(), Charset.defaultCharset());
        Matcher matcher = cssVarRe.matcher(contents);
        String replaced = matcher.replaceAll("currentColor");
        return new ByteArrayInputStream(replaced.getBytes());
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
                SVGLoader svgLoader = new SVGLoader();
                SVGDocument svgDocument = svgLoader.load(canonicalPathToByteArrayInputStream(canonicalPath));
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
            logDebug(e, canonicalFile);
        }
        return null;
    }

    private BufferedImage read(@NotNull VirtualFile canonicalFile) throws IOException {
        return ImageIO.read(new File(canonicalFile.getPath()));
    }

    private void logDebug(Exception e, @NotNull VirtualFile virtualFile) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Error loading preview Icon - " + virtualFile.getCanonicalPath(), e);
        }
    }

    private String getExtendedImgFormats() {
        return extendedImgFormats.get().toString();
    }

    private static Image scaleImage(Image image) {
        return ImageUtil.scaleImage(image, SCALING_SIZE, SCALING_SIZE);
    }
}
