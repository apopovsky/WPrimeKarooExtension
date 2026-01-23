# docker-build.ps1
# Runs the Android build inside a Docker container with caching enabled.

Write-Output "Starting Docker build..."
Write-Output "This uses volume 'wprime_gradle_cache' to cache downloads."

docker run --rm `
    -v "${PWD}:/home/circleci/project" `
    -v "wprime_gradle_cache:/home/circleci/.gradle" `
    -w "/home/circleci/project" `
    cimg/android:2025.01 `
    /bin/bash -c "sed -i 's/\r$//' gradlew && ./gradlew assembleDebug"

if ($LASTEXITCODE -eq 0) {
    Write-Output "Build Successful! APK is in app/build/outputs/apk/debug/"
} else {
    Write-Output "Build Failed!"
}
