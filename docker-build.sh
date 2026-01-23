#!/bin/bash
# docker-build.sh
# Runs the Android build inside a Docker container with caching enabled.

echo "Starting Docker build..."
echo "This uses volume 'wprime_gradle_cache' to cache downloads."

docker run --rm \
    -v "$(pwd):/home/circleci/project" \
    -v "wprime_gradle_cache:/home/circleci/.gradle" \
    -w "/home/circleci/project" \
    cimg/android:2025.01 \
    /bin/bash -c "sed -i 's/\r$//' gradlew && ./gradlew assembleDebug"

if [ $? -eq 0 ]; then
    echo "Build Successful! APK is in app/build/outputs/apk/debug/"
else
    echo "Build Failed!"
fi
