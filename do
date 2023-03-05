#!/bin/bash

((nextParam = 1))
for ((cmd = 1; cmd <= $#; cmd++)); do

    ((nextParam++))

    case "${!cmd}" in

    "help")
        echo "w \$V:    set gradle wrapper"
        echo "fixgit:  fix permission flag on git index for required files"
        echo "run :    run plugin in IntelliJ"
        echo "runeap:  run plugin in latest IntelliJ EAP Snapshot"
        echo "release: package plugin"
        echo "cv:      check dependencies and Gradle updates"
        echo "oga:     check for deprecated groupId and artifactId coordinates"
        ;;

    "w")
        gradle wrapper --gradle-version=%2 --no-daemon
        ;;

    "fixgit")
        git update-index --chmod=+x "gradlew"
        echo "'gradlew' has now executable flag on git gradlew"
        git update-index --chmod=+x "do"
        echo "'do' has now executable flag on git index"
        ;;

    "run")
        ./gradlew buildPlugin runIde --warning-mode all
        ;;

    "runeap")
        ./gradlew buildPlugin runIde --warning-mode all -PpluginIdeaVersion=IC-LATEST-EAP-SNAPSHOT -PpluginDownloadIdeaSources=false
        ;;

    "release")
        ./gradlew clean buildPlugin verifyPlugin --warning-mode all
        ;;

    "cv")
        ./gradlew dependencyUpdates --warning-mode all
        ;;

    "oga")
        ./gradlew gradlew biz-lermitage-oga-gradle-check --warning-mode all
        ;;

    esac

done
