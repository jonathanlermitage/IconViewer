# Icon Viewer 2 Change Log

## 1.31.3 (2025/04/25)
* upgrade JSVG (SVG renderer) to 2.0.0.

## 1.31.2 (2025/01/25)
* upgrade JSVG (SVG renderer) to 1.7.0.

## 1.31.1 (2024/10/28)
* upgrade TwelveMonkeys library to 3.12.0.

## 1.31.0 (2024/08/20)
* upgrade JSVG (SVG renderer) to 1.6.1.
* fix usage of a deprecated JSVG API.

## 1.30.0 (2024/06/14)
* integrate the new IDE [Exception Analyzer](https://plugins.jetbrains.com/docs/marketplace/exception-analyzer.html). This is an easy way to report plugin exceptions from IntelliJ Platform-based products to plugin developers right on JetBrains Marketplace, instead of opening an issue on the plugin's GitHub repository.

## 1.22.9 (2024/06/08)
* upgrade JSVG (SVG renderer) to 1.5.0.
* upgrade TwelveMonkeys library to 3.11.0: basically, various fixes for JPEG, PSD and TIFF files support.

## 1.22.8 (2024/02/16)
* upgrade JSVG (SVG renderer) to 1.4.0.

## 1.22.7 (2023/12/07)
* upgrade JSVG (SVG renderer) to 1.3.0.

## 1.22.6 (2023/11/25)
* upgrade TwelveMonkeys library to [3.10.1](https://github.com/haraldk/TwelveMonkeys/releases/tag/twelvemonkeys-3.10.1): basically, various fixes for JPEG and PSD files support.

## 1.22.5 (2023/10/24)
* upgrade TwelveMonkeys library to [3.10.0](https://github.com/haraldk/TwelveMonkeys/releases/tag/twelvemonkeys-3.10.0): basically, various fixes for JPEG, PSD and TIFF files support.

## 1.22.4 (2023/10/13)
* upgrade JSVG (SVG renderer) to 1.2.0.

## 1.22.3 (2023/09/17)
* fix a regression with classloader. **This update is highly recommended**.
* upgrade JSVG (SVG renderer) to 1.1.0.

## 1.22.2 (2023/09/03)
* don't generate error report when loading invalid SVG file.
* logs are now less verbose.

## 1.22.1 (2023/09/03)
* fix an IDE freeze when rendering many SVG files.

## 1.22.0 (2023/09/02)
* replace Apache Batik by JSVG for SVG rendering. Batik integration potentially had some unwanted side effects on IDE.

## 1.21.1 (2023/05/22)
* remove JSVG and restore Apache Batik for SVG rendering. Batik offers better support of SVG files than JSVG.
* fix usage of Apache Batik (fixed a classloader issue, which could impact other plugins).

## 1.21.0 (2023/05/21)
* fix SVG support with IDE 2023.2 EAP.
* important code rework, replaced Apache Batik by JSVG for SVG rendering.

## 1.20.0 (2023/03/07)
* fix SVG support with IDE 2023.1 EAP.
* important code rework.

## 1.19.2 (2023/03/04)
* revert 1.19.1 fix as it may break 2022.3.2, and I found some test cases that break IDE 2023.1 EAP too. It seems that IDE 2023.1 EAP broke SVG support...

## 1.19.1 (2023/02/28)
* fix SVG support with IDE 2023.1 EAP.

## 1.19.0 (2022/11/26)
* upgrade TwelveMonkeys library to [3.9.4](https://github.com/haraldk/TwelveMonkeys/releases/tag/twelvemonkeys-3.9.4): various fixes.

## 1.18.0 (2022/10/22)
* upgrade TwelveMonkeys library to [3.9.3](https://github.com/haraldk/TwelveMonkeys/releases/tag/twelvemonkeys-3.9.3): fixes WebP alpha decoding bug.

## 1.17.0 (2022/10/21)
* upgrade TwelveMonkeys library to [3.9.2](https://github.com/haraldk/TwelveMonkeys/releases/tag/twelvemonkeys-3.9.2): fixes a serious performance issue in the lossless WebP decoding.

## 1.16.0 (2022/10/15)
* upgrade TwelveMonkeys library to [3.9.0](https://github.com/haraldk/TwelveMonkeys/releases/tag/twelvemonkeys-3.9.0).

## 1.15.0 (2022/08/20)
* upgrade TwelveMonkeys library to [3.8.3](https://github.com/haraldk/TwelveMonkeys/releases/tag/twelvemonkeys-3.8.3).

## 1.14.0 (2022/02/23)
* upgrade TwelveMonkeys library to 3.8.2.

## 1.13.0 (2021/12/28)
* upgrade TwelveMonkeys library to 3.8.1.

## 1.12.0 (2021/12/13)
* upgrade TwelveMonkeys library to 3.8.0.

## 1.11.0 (2021/11/26)
* improve SVG support. Thx [cherepanov](https://github.com/jonathanlermitage/IconViewer/pull/4).

## 1.10.0 (2021/07/01)
* improve rendering quality of SVG files.
* internal: minor performance optimization.

## 1.9.0 (2021/05/05)
* internal: removed dependencies to IJ internal code used to load SVG files. Replaced by TwelveMonkeys + Apache Batik libraries. This way, future IDE updates won't break this plugin.

## 1.8.0 (2021/04/25)
* upgrade TwelveMonkeys library from 3.6.4 to [3.7.0](https://github.com/haraldk/TwelveMonkeys/releases/tag/twelvemonkeys-3.7.0).

## 1.7.1 (2021/03/14)
* fix [#2](https://github.com/jonathanlermitage/IconViewer/issues/2): error when opening an invalid SVG file.

## 1.7.0 (2021/03/08)
* upgrade TwelveMonkeys library from 3.6.3 to [3.6.4](https://github.com/haraldk/TwelveMonkeys/releases/tag/twelvemonkeys-3.6.4): Fix infinite loops in corrupted JPEGs.

## 1.6.0 (2021/03/03)
* upgrade TwelveMonkeys library from 3.6.2 to [3.6.3](https://github.com/haraldk/TwelveMonkeys/releases/tag/twelvemonkeys-3.6.3).

## 1.5.1 (2021/01/27)
* render non-square SVG images, don't throw an exception.

## 1.5.0 (2021/01/25)
* upgrade TwelveMonkeys library from 3.6.1 to [3.6.2](https://github.com/haraldk/TwelveMonkeys/releases/tag/twelvemonkeys-3.6.2).

## 1.4.1 (2020/11/30)
* fix [#1](https://github.com/jonathanlermitage/IconViewer/issues/1): I/O error reading PNG header.

## 1.4.0 (2020/11/29)
* upgrade TwelveMonkeys library from 3.6 to [3.6.1](https://github.com/haraldk/TwelveMonkeys/releases/tag/twelvemonkeys-3.6.1).
* fix usage of TwelveMonkeys library, it should fix support of many file formats.
* code cleanup and minor performance improvements.

## 1.3.0 (2020/07/18)
* upgrade TwelveMonkeys library from 3.5 to [3.6](https://github.com/haraldk/TwelveMonkeys/releases/tag/twelvemonkeys-3.6).

## 1.2.0 (2020/04/08)
* support more image formats by using [TwelveMonkeys library](https://github.com/haraldk/TwelveMonkeys). Thx [JetBrains support](https://youtrack.jetbrains.com/issue/IDEA-236055#focus=streamItem-27-4037903.0-0).

## 1.1.0 (2020/03/26)
* improved images rendering quality.

## 1.0.0 (2020/03/24)
* first release, based on IconViewer 1.15.
* project migrated to Gradle.
* fix [issue #9: ArrayIndexOutOfBoundsException](https://github.com/davidsommer/IconViewer/issues/9).
