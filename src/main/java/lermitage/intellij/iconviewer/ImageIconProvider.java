package lermitage.intellij.iconviewer;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.ImageUtil;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.SVGConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.DOMImplementation;

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
import java.util.*;
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
    private static final float SVG_SIZE_BEFORE_RESCALING = 128f;
    private static final Pattern cssVarRe = Pattern.compile("var\\([-\\w]+\\)");

    private final ThreadLocal<TranscodingHints> localTranscoderHints = ThreadLocal.withInitial(() -> null);
    private final ThreadLocal<DOMImplementation> localSVGDOMImplementation = ThreadLocal.withInitial(() -> null);
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
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                ImageIO.scanForPlugins();
            } finally {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
            localContextUpdated.set(true);
            extendedImgFormats.set(Stream.of(ImageIO.getReaderFormatNames()).map(String::toLowerCase).collect(Collectors.toSet()));
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
                // https://stackoverflow.com/questions/11435671/how-to-get-a-bufferedimage-from-a-svg
                TranscodingHints transcoderHints = localTranscoderHints.get();
                if (transcoderHints == null) {
                    transcoderHints = new TranscodingHints();
                    transcoderHints.put(ImageTranscoder.KEY_HEIGHT, SVG_SIZE_BEFORE_RESCALING);
                    transcoderHints.put(ImageTranscoder.KEY_WIDTH, SVG_SIZE_BEFORE_RESCALING);
                    transcoderHints.put(ImageTranscoder.KEY_XML_PARSER_VALIDATING, false);
                    transcoderHints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI, SVGConstants.SVG_NAMESPACE_URI);
                    transcoderHints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT, "svg");

                    DOMImplementation domImplementation = localSVGDOMImplementation.get();
                    if (domImplementation == null) {
                        domImplementation = SVGDOMImplementation.getDOMImplementation();
                        localSVGDOMImplementation.set(domImplementation);
                    }
                    transcoderHints.put(ImageTranscoder.KEY_DOM_IMPLEMENTATION, domImplementation);

                    localTranscoderHints.set(transcoderHints);
                }
                ByteArrayInputStream inputStream = canonicalPathToByteArrayInputStream(canonicalPath);
                TranscoderInput transcoderInput = new TranscoderInput(inputStream);
                BufferedImage[] imagePointer = new BufferedImage[1];
                ImageTranscoder t = new ImageTranscoder() {

                    @Override
                    public BufferedImage createImage(int w, int h) {
                        return ImageUtil.createImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    }

                    @Override
                    public void writeImage(BufferedImage bufferedImage, TranscoderOutput transcoderOutput) {
                        imagePointer[0] = bufferedImage;
                    }
                };
                t.setTranscodingHints(transcoderHints);
                t.transcode(transcoderInput, null);
                BufferedImage bufferedImage = imagePointer[0];
                Image thumbnail = scaleImage(bufferedImage);
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
