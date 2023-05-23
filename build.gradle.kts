import com.github.benmanes.gradle.versions.reporter.PlainTextReporter
import com.github.benmanes.gradle.versions.reporter.result.Result
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.palantir.gradle.gitversion.VersionDetails
import groovy.lang.Closure
import org.jetbrains.changelog.Changelog

plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.13.3" // https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.changelog") version "2.0.0" // https://github.com/JetBrains/gradle-changelog-plugin
    id("com.github.ben-manes.versions") version "0.46.0" // https://github.com/ben-manes/gradle-versions-plugin
    id("com.palantir.git-version") version "3.0.0" // https://github.com/palantir/gradle-git-version
    id("biz.lermitage.oga") version "1.1.1"
}

// Import variables from gradle.properties file
val pluginIdeaVersion: String by project
val pluginDownloadIdeaSources: String by project
val pluginInstrumentPluginCode: String by project
val pluginVersion: String by project
val pluginJavaVersion: String by project

version = if (pluginVersion == "auto") {
    val versionDetails: Closure<VersionDetails> by extra
    val lastTag = versionDetails().lastTag
    if (lastTag.startsWith("v", ignoreCase = true)) {
        lastTag.substring(1)
    } else {
        lastTag
    }
} else {
    pluginVersion
}

logger.quiet("Will use IDEA $pluginIdeaVersion and Java $pluginJavaVersion. Plugin version set to $version.")

group = "lermitage.intellij.iconviewer"
version = version

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

    implementation("com.twelvemonkeys.imageio:imageio-batik:$twelvemonkeysVersion") // SVG support
    implementation("org.apache.xmlgraphics:batik-anim:1.16")
    implementation("org.apache.xmlgraphics:batik-transcoder:1.16")
    implementation("xerces:xercesImpl:2.12.2")
}

intellij {
    downloadSources.set(pluginDownloadIdeaSources.toBoolean() && !System.getenv().containsKey("CI"))
    instrumentCode.set(true)
    pluginName.set("Icon Viewer 2")
    sandboxDir.set("${rootProject.projectDir}/.idea-sandbox/${shortenIdeVersion(pluginIdeaVersion)}")
    updateSinceUntilBuild.set(false)
    version.set(pluginIdeaVersion)
}

changelog {
    headerParserRegex.set("(.*)".toRegex())
    itemPrefix.set("*")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = pluginJavaVersion
        targetCompatibility = pluginJavaVersion
        options.compilerArgs = listOf("-Xlint:deprecation")
        options.encoding = "UTF-8"
    }
    withType<DependencyUpdatesTask> {
        checkForGradleUpdate = true
        gradleReleaseChannel = "current"
        revision = "release"
        rejectVersionIf {
            isNonStable(candidate.version)
        }
        outputFormatter = closureOf<Result> {
            unresolved.dependencies.removeIf {
                val coordinates = "${it.group}:${it.name}"
                coordinates.startsWith("unzipped.com") || coordinates.startsWith("com.jetbrains:ideaI")
            }
            PlainTextReporter(project, revision, gradleReleaseChannel)
                .write(System.out, this)
        }
    }
    runIde {
        maxHeapSize = "1g" // https://docs.gradle.org/current/dsl/org.gradle.api.tasks.JavaExec.html

        // force detection of slow operations in EDT when playing with sandboxed IDE (SlowOperations.assertSlowOperationsAreAllowed)
        jvmArgs("-Dide.slow.operations.assertion=true")

        autoReloadPlugins.set(false)

        // If any warning or error with missing --add-opens, wait for the next gradle-intellij-plugin's update that should sync
        // with https://raw.githubusercontent.com/JetBrains/intellij-community/master/plugins/devkit/devkit-core/src/run/OpenedPackages.txt
        // or do it manually
    }
    buildSearchableOptions {
        enabled = false
    }
    patchPluginXml {
        changeNotes.set(provider {
            with(changelog) {
                renderItem(getLatest(), Changelog.OutputType.HTML)
            }
        })
    }
}

fun isNonStable(version: String): Boolean {
    if (listOf("RELEASE", "FINAL", "GA").any { version.uppercase().endsWith(it) }) {
        return false
    }
    return listOf("alpha", "Alpha", "ALPHA", "b", "beta", "Beta", "BETA", "rc", "RC", "M", "EA", "pr", "atlassian").any {
        "(?i).*[.-]${it}[.\\d-]*$".toRegex().matches(version)
    }
}

/** Return an IDE version string without the optional PATCH number.
 * In other words, replace IDE-MAJOR-MINOR(-PATCH) by IDE-MAJOR-MINOR. */
fun shortenIdeVersion(version: String): String {
    if (version.contains("SNAPSHOT", ignoreCase = true)) {
        return version
    }
    val matcher = Regex("[A-Za-z]+[\\-]?[0-9]+[\\.]{1}[0-9]+")
    return try {
        matcher.findAll(version).map { it.value }.toList()[0]
    } catch (e: Exception) {
        logger.warn("Failed to shorten IDE version $version: ${e.message}")
        version
    }
}
