import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.10.0" // https://github.com/JetBrains/gradle-intellij-plugin and https://lp.jetbrains.com/gradle-intellij-plugin/
    id("com.github.ben-manes.versions") version "0.42.0" // https://github.com/ben-manes/gradle-versions-plugin
}

// Import variables from gradle.properties file
val pluginIdeaVersion: String by project
val pluginDownloadIdeaSources: String by project
val pluginInstrumentPluginCode: String by project
val pluginVersion: String by project
val pluginJavaVersion: String by project

val inCI = System.getenv("CI") != null

logger.quiet("Will use IDEA $pluginIdeaVersion and Java $pluginJavaVersion")

group = "lermitage.intellij.iconviewer"
version = pluginVersion

repositories {
    mavenCentral()
}

val twelvemonkeysVersion = "3.9.4"

dependencies {
    implementation("com.twelvemonkeys.imageio:imageio-core:$twelvemonkeysVersion") // https://github.com/haraldk/TwelveMonkeys/releases
    implementation("com.twelvemonkeys.imageio:imageio-bmp:$twelvemonkeysVersion")  // https://github.com/haraldk/TwelveMonkeys/wiki/BMP-Plugin
    implementation("com.twelvemonkeys.imageio:imageio-hdr:$twelvemonkeysVersion")  // https://github.com/haraldk/TwelveMonkeys/wiki/HDR-Plugin
    implementation("com.twelvemonkeys.imageio:imageio-icns:$twelvemonkeysVersion") // https://github.com/haraldk/TwelveMonkeys/wiki/ICNS-Plugin
    implementation("com.twelvemonkeys.imageio:imageio-iff:$twelvemonkeysVersion")  // https://github.com/haraldk/TwelveMonkeys/wiki/IFF-Plugin
    implementation("com.twelvemonkeys.imageio:imageio-jpeg:$twelvemonkeysVersion") // https://github.com/haraldk/TwelveMonkeys/wiki/JPEG-Plugin
    implementation("com.twelvemonkeys.imageio:imageio-pcx:$twelvemonkeysVersion")  // https://github.com/haraldk/TwelveMonkeys/wiki/PCX-Plugin
    implementation("com.twelvemonkeys.imageio:imageio-pict:$twelvemonkeysVersion") // https://github.com/haraldk/TwelveMonkeys/wiki/PICT-Plugin
    implementation("com.twelvemonkeys.imageio:imageio-pnm:$twelvemonkeysVersion")  // https://github.com/haraldk/TwelveMonkeys/wiki/PNM-Plugin
    implementation("com.twelvemonkeys.imageio:imageio-psd:$twelvemonkeysVersion")  // https://github.com/haraldk/TwelveMonkeys/wiki/PSD-Plugin
    implementation("com.twelvemonkeys.imageio:imageio-sgi:$twelvemonkeysVersion")  // https://github.com/haraldk/TwelveMonkeys/wiki/SGI-Plugin
    implementation("com.twelvemonkeys.imageio:imageio-tga:$twelvemonkeysVersion")  // https://github.com/haraldk/TwelveMonkeys/wiki/TGA-Plugin
    implementation("com.twelvemonkeys.imageio:imageio-tiff:$twelvemonkeysVersion") // https://github.com/haraldk/TwelveMonkeys/wiki/TIFF-Plugin
    // TODO Apache Batik is bundled with IJ and IJ-based IDEs (tested with PyCharm Community). If needed, see how to
    //  integrate org.apache.xmlgraphics:batik-all:1.14 without failing to load org.apache.batik.anim.dom.SAXSVGDocumentFactory
    implementation("com.twelvemonkeys.imageio:imageio-batik:$twelvemonkeysVersion") // SVG support
}

intellij {
    downloadSources.set(pluginDownloadIdeaSources.toBoolean() && !inCI)
    instrumentCode.set(pluginInstrumentPluginCode.toBoolean())
    pluginName.set("Icon Viewer 2")
    sandboxDir.set("${rootProject.projectDir}/.idea-sandbox/${shortIdeVersion(pluginIdeaVersion)}")
    updateSinceUntilBuild.set(false)
    version.set(pluginIdeaVersion)
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = pluginJavaVersion
        targetCompatibility = pluginJavaVersion
        options.compilerArgs = listOf("-Xlint:deprecation")
    }
    withType<DependencyUpdatesTask> {
        checkForGradleUpdate = true
        gradleReleaseChannel = "current"
        outputFormatter = "plain"
        outputDir = "build"
        reportfileName = "dependencyUpdatesReport"
        revision = "release"
        resolutionStrategy {
            componentSelection {
                all {
                    if (isNonStable(candidate.version)) {
                        logger.debug(" - [ ] ${candidate.module}:${candidate.version} candidate rejected")
                        reject("Not stable")
                    } else {
                        logger.debug(" - [X] ${candidate.module}:${candidate.version} candidate accepted")
                    }
                }
            }
        }
    }
    runIde {
        jvmArgs("-Xms128m")
        jvmArgs("-Xmx1024m")
    }
    buildSearchableOptions {
        enabled = false
    }
}

fun isNonStable(version: String): Boolean {
    if (listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().endsWith(it) }) {
        return false
    }
    return listOf("alpha", "Alpha", "ALPHA", "b", "beta", "Beta", "BETA", "rc", "RC", "M", "EA", "pr", "atlassian").any {
        "(?i).*[.-]${it}[.\\d-]*$".toRegex().matches(version)
    }
}

/** Return an IDE version string without the optional PATCH number.
 * In other words, replace IDE-MAJOR-MINOR(-PATCH) by IDE-MAJOR-MINOR. */
fun shortIdeVersion(version: String): String {
    val matcher = Regex("[A-Za-z]+[\\-]?[0-9]+[\\.]{1}[0-9]+")
    return try {
        matcher.findAll(version).map { it.value }.toList()[0]
    } catch (e: Exception) {
        logger.warn("Failed to shorten IDE version $version", e)
        version
    }
}
