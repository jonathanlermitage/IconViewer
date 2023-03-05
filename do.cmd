@echo off

if [%1] == [help] (
  echo w $V:    set gradle wrapper
  echo fixgit:  fix permission flag on git index for required files
  echo run :    run plugin in IntelliJ
  echo runeap:  run plugin in latest IntelliJ EAP Snapshot
  echo release: package plugin
  echo cv:      check dependencies and Gradle updates
  echo oga:     check for deprecated groupId and artifactId coordinates
)

if [%1] == [w] (
  gradle wrapper --gradle-version=%2 --no-daemon
)
if [%1] == [fixgit] (
  git update-index --chmod=+x gradlew
  echo "gradlew" has now executable flag on git
  git update-index --chmod=+x do
  echo "do" has now executable flag on git
)
if [%1] == [run] (
  gradlew buildPlugin runIde --warning-mode all
)
if [%1] == [runeap] (
  gradlew buildPlugin runIde --warning-mode all -PpluginIdeaVersion=IC-LATEST-EAP-SNAPSHOT -PpluginDownloadIdeaSources=false
)
if [%1] == [release] (
  gradlew clean buildPlugin verifyPlugin --warning-mode all
)
if [%1] == [cv] (
  gradlew dependencyUpdates --warning-mode all
)
if [%1] == [oga] (
  gradlew biz-lermitage-oga-gradle-check --warning-mode all
)
