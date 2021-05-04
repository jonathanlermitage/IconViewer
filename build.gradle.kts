import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("java")
    id("idea")
    id("org.jetbrains.intellij") version "0.7.3" // https://github.com/JetBrains/gradle-intellij-plugin
    id("com.github.ben-manes.versions") version "0.38.0" // https://github.com/ben-manes/gradle-versions-plugin
}

// Import variables from gradle.properties file
val pluginIdeaVersion: String by project
val pluginDownloadIdeaSources: String by project
val pluginInstrumentPluginCode: String by project
val pluginVersion: String by project
val pluginJavaVersion: String by project
val pluginEnableBuildSearchableOptions: String by project

val twelvemonkeysVersion = "3.7.0"

val inCI = System.getenv("CI") != null

println("Will use IDEA $pluginIdeaVersion and Java $pluginJavaVersion")

group = "lermitage.intellij.iconviewer"
version = pluginVersion

repositories {
    mavenCentral()
}

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
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
}

intellij {
    downloadSources = pluginDownloadIdeaSources.toBoolean() && !inCI
    instrumentCode = pluginInstrumentPluginCode.toBoolean()
    pluginName = "Icon Viewer 2"
    sandboxDirectory = "${rootProject.projectDir}/.idea-sandbox/${pluginIdeaVersion}"
    updateSinceUntilBuild = false
    version = pluginIdeaVersion
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = pluginJavaVersion
        targetCompatibility = pluginJavaVersion
        options.compilerArgs = listOf("-Xlint:deprecation")
    }
    withType<Test> {
        useJUnitPlatform()
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
                        println(" - [ ] ${candidate.module}:${candidate.version} candidate rejected")
                        reject("Not stable")
                    } else {
                        println(" - [X] ${candidate.module}:${candidate.version} candidate accepted")
                    }
                }
            }
        }
    }
    runIde {
        jvmArgs = listOf("-Xms768m", "-Xmx2048m", "--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED")
    }
    buildSearchableOptions {
        enabled = pluginEnableBuildSearchableOptions.toBoolean()
        jvmArgs = listOf("--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED")
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
