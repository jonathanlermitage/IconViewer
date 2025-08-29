<h1 align="center">
    <img src="./src/main/resources/META-INF/pluginIcon.svg" width="84" height="84" alt="logo"/><br/>
    Icon Viewer 2
</h1>

<p align="center">
    <a href="https://plugins.jetbrains.com/plugin/13995-icon-viewer-2"><img src="https://img.shields.io/jetbrains/plugin/v/13995-icon-viewer-2.svg"/></a>
    <a href="https://plugins.jetbrains.com/plugin/13995-icon-viewer-2"><img src="https://img.shields.io/jetbrains/plugin/d/13995-icon-viewer-2.svg"/></a>
    <a href="https://github.com/jonathanlermitage/IconViewer/blob/master/LICENSE.txt"><img src="https://img.shields.io/github/license/jonathanlermitage/IconViewer.svg"/></a>
</p>

Intellij IDEA 2023.1+  (Community and Ultimate) plugin that allows you to preview project images as an icon in your project explorer.  
Helpful if you have a lot of icons in your project, Icon Viewer 2 gives you a nice preview icon. 
  
It works with all JetBrains products like WebStorm, DataGrip, etc.

Download plugin on [GitHub](https://github.com/jonathanlermitage/IconViewer/releases), [JetBrains Plugins Repository](https://plugins.jetbrains.com/plugin/13995-icon-viewer-2) or via IntelliJ IDEA (<kbd>File</kbd>, <kbd>Settings</kbd>, <kbd>Plugins</kbd>, <kbd>Browse repositories...</kbd>, <kbd>Icon Viewer 2</kbd>).

## Author

* Owner of this fork: Jonathan Lermitage (<jonathan.lermitage@gmail.com>, [linkedin](https://www.linkedin.com/in/jonathan-lermitage-092711142/))
* Owner of the original project: [David Sommer](https://github.com/davidsommer/IconViewer).

## Build

### Gradle commands

* build plugin: `./gradlew buildPlugin`. See generated zip located into `build/distribution/`.
* run IDE with plugin: `./gradlew runIde`.

You can also look at the [Makefile](./Makefile).

## Contribution

Open an issue or a pull-request. Contributions should be tested on [master](https://github.com/jonathanlermitage/IconViewer) branch.  
Please reformat new and modified code only: do not reformat the whole project or entire existing file (in other words, try to limit the amount of changes in order to speed up code review).  
To finish, don't hesitate to add your name or nickname to contributors list ;-)

## License

MIT License. In other words, you can do what you want: this project is entirely OpenSource, Free and Gratis.

## Demonstration

Images are scaled to 16x16.

Supported file formats are:

 - *.bmp
 - *.gif
 - *.jpg
 - *.jpeg
 - *.png 
 - *.svg (tip: if *Extra Icons* plugin is active, you may want to disable its SVG support in order to diplay SVG files in place of SVG logo)
 
Supported when *Android Support* plugin is activated:

 - *.webm
 - *.webp
 
Files Supported by bundled TwelveMonkeys library, like *.ico, *.tga, etc. See [File formats supported](https://github.com/haraldk/TwelveMonkeys#file-formats-supported).

You can also filter the images to display. To proceed, go to `Help > Edit Custom Properties` then add a line like `icon-viewer-2-max-filesize=X` where X is a file size in kilobytes (KB). Example: `icon-viewer-2-max-filesize=1024` will tell Icon Viewer 2 to display images only for files smaller than 1MB. Bigger images will display the default logo for image files. This property takes effect after the IDE restart.
 
### Screenshot: 

![screeshot](misc/screenshot.png)
