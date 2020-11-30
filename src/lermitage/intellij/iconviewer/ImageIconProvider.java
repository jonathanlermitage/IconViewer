package lermitage.intellij.iconviewer;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBImageIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
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

    private final ThreadLocal<Boolean> contextUpdated = ThreadLocal.withInitial(() -> false);

    /** Image formats supported by TwelveMonkeys. */
    private final ThreadLocal<Set<String>> extendedImgFormats = ThreadLocal.withInitial(Collections::emptySet);
    /** Image formats supported when Android plugin is enabled. */
    private final Set<String> androidImgFormats = new HashSet<>(Arrays.asList("webm", "webp"));
    /** SVG image format. */
    private final Set<String> svgImgFormats = new HashSet<>(Collections.singletonList("svg"));

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
            } else if (svgImgFormats.contains(fileExtension)) {
                return previewSvgImage(canonicalFile);
            } else {
                return previewImageWithExtendedSupport(canonicalFile, fileExtension);
            }
        }
        return null;
    }

    @Nullable
    private Icon previewSvgImage(@NotNull VirtualFile canonicalFile) {
        Image image = CustomIconLoader.loadSVGFromVirtualFile(canonicalFile);
        if (image == null) {
            return null;
        }
        return IconUtil.createImageIcon(image);
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

    @Nullable
    private Icon previewImageWithExtendedSupport(@NotNull VirtualFile canonicalFile, @NotNull String fileExtension) {
        try {
            if (!contextUpdated.get()) {
                Thread.currentThread().setContextClassLoader(ImageIconProvider.class.getClassLoader());
                ImageIO.scanForPlugins();
                contextUpdated.set(true);
                extendedImgFormats.set(Stream.of(ImageIO.getReaderFormatNames()).map(String::toLowerCase).collect(Collectors.toSet()));
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("New ImageIconProvider thread - Image file formats supported by " +
                        "Twelvemonkeys library: " + getExtendedImgFormats());
                }
            }
            if (!extendedImgFormats.get().contains(fileExtension)) {
                return null;
            }
            String canonicalPath = canonicalFile.getCanonicalPath();
            if (canonicalPath == null) {
                return null;
            }
            try (ImageInputStream input = ImageIO.createImageInputStream(new File(canonicalPath))) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                while (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(input);
                        BufferedImage originalImage = reader.read(0);
                        Image thumbnail = originalImage.getScaledInstance(16, 16, BufferedImage.SCALE_SMOOTH);
                        if (thumbnail != null) {
                            return new JBImageIcon(thumbnail);
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
}
