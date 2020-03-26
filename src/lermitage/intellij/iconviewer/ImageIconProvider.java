package lermitage.intellij.iconviewer;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IconUtil;
import com.intellij.util.ImageLoader;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBImageIcon;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by David Sommer on 19.05.17.
 *
 * @author davidsommer
 * @author jonathanlermitage
 */
public class ImageIconProvider extends IconProvider {

    private static final Logger LOGGER = Logger.getInstance(ImageIconProvider.class);
    private static final int SCALING_SIZE = 16;

    public Icon getIcon(@NotNull PsiElement psiElement, int flags) {
        PsiFile containingFile = psiElement.getContainingFile();
        if (checkImagePath(containingFile)) {
            VirtualFile canonicalFile = containingFile.getVirtualFile().getCanonicalFile();
            if (containingFile.getVirtualFile().getExtension() != null) {
                switch (containingFile.getVirtualFile().getExtension().toLowerCase()) {

                    case "svg":
                        CustomIconLoader.ImageWrapper imageWrapper = CustomIconLoader.loadFromVirtualFile(containingFile.getVirtualFile());
                        if (imageWrapper == null) {
                            return null;
                        }
                        CustomIconLoader.ImageWrapper fromBase64 = CustomIconLoader.fromBase64(CustomIconLoader.toBase64(imageWrapper), IconType.SVG);
                        if (fromBase64 == null) {
                            return null;
                        }
                        return IconUtil.createImageIcon(fromBase64.getImage());

                    case "webm":
                    case "webp":
                        if (canonicalFile == null) {
                            return null;
                        }

                        try {
                            return new ImageIcon(ImageIO.read(new File(canonicalFile.getPath()))
                                .getScaledInstance(SCALING_SIZE, SCALING_SIZE, BufferedImage.SCALE_SMOOTH));
                        } catch (IOException e) {
                            logWarn(e, canonicalFile);
                            return null;
                        }

                    default:
                        if (canonicalFile == null) {
                            return null;
                        }

                        try {
                            Image img = ImageLoader.loadFromBytes(canonicalFile.contentsToByteArray());
                            Image image = ImageUtil.scaleImage(img, SCALING_SIZE, SCALING_SIZE);
                            if (image != null) {
                                JBImageIcon jbImageIcon = new JBImageIcon(image);
                                if (jbImageIcon.getImage() != null && jbImageIcon.getIconHeight() > 0 && jbImageIcon.getIconWidth() > 0) {
                                    return jbImageIcon;
                                }
                            }
                        } catch (Exception e) {
                            logWarn(e, canonicalFile);
                        }

                        try {
                            return new ImageIcon(ImageIO.read(new File(canonicalFile.getPath()))
                                .getScaledInstance(SCALING_SIZE, SCALING_SIZE, BufferedImage.SCALE_SMOOTH));
                        } catch (IOException e) {
                            logWarn(e, canonicalFile);
                            return null;
                        }
                }
            }
        }
        return null;
    }

    private void logWarn(Exception e, VirtualFile virtualFile) {
        LOGGER.warn("Error loading preview Icon - " + virtualFile.getCanonicalPath(), e);
    }

    private boolean checkImagePath(PsiFile containingFile) {
        return containingFile != null
            && containingFile.getVirtualFile() != null
            && containingFile.getVirtualFile().getCanonicalFile() != null
            && containingFile.getVirtualFile().getCanonicalFile().getCanonicalPath() != null
            && UIUtils.isImageFile(containingFile.getName())
            && !containingFile.getVirtualFile().getCanonicalFile().getCanonicalPath().contains(".jar");
    }
}
