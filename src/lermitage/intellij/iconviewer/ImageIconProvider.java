package lermitage.intellij.iconviewer;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.swing.*;
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

    public Icon getIcon(@NotNull PsiElement psiElement, int flags) {
        PsiFile containingFile = psiElement.getContainingFile();
        if (checkImagePath(containingFile)) {
            VirtualFile canonicalFile = containingFile.getVirtualFile().getCanonicalFile();
            if (containingFile.getVirtualFile().getExtension() != null) {
                IconType imgType = containingFile.getVirtualFile().getExtension().equalsIgnoreCase("svg") ? IconType.SVG : IconType.IMG;
                switch (imgType) {
                    case IMG:
                        if (canonicalFile == null) {
                            return null;
                        }
                        try {
                            return new ImageIcon(ImageIO.read(new File(canonicalFile.getPath())).getScaledInstance(16, 16, BufferedImage.SCALE_SMOOTH));
                        } catch (IOException e) {
                            LOGGER.warn("Error loading preview Icon - " + canonicalFile.getCanonicalPath(), e);
                            return null;
                        }
                    case SVG:
                        CustomIconLoader.ImageWrapper imageWrapper = CustomIconLoader.loadFromVirtualFile(containingFile.getVirtualFile());
                        if (imageWrapper == null) {
                            return null;
                        }
                        CustomIconLoader.ImageWrapper fromBase64 = CustomIconLoader.fromBase64(CustomIconLoader.toBase64(imageWrapper), imgType);
                        if (fromBase64 == null) {
                            return null;
                        }
                        return IconUtil.createImageIcon(fromBase64.getImage());
                }
            }
        }
        return null;
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
