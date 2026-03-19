#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BUILD_FILE="$ROOT_DIR/app/build.gradle.kts"

# Extract version info from build.gradle.kts
VERSION_NAME=$(grep 'versionName' "$BUILD_FILE" | sed 's/.*"\(.*\)\".*/\1/')
VERSION_CODE=$(grep 'versionCode' "$BUILD_FILE" | sed 's/[^0-9]*//g')

if [[ -z "$VERSION_NAME" || -z "$VERSION_CODE" ]]; then
  echo "Error: Could not read versionName or versionCode from $BUILD_FILE"
  exit 1
fi

echo "==> Building release APK..."
# Ensure download directory exists
mkdir -p "$ROOT_DIR"/download

# Clean old APKs in download directory
rm -f "$ROOT_DIR"/download/*.apk

# Build release APK
"$ROOT_DIR"/gradlew assembleRelease

# Find the built APK (assuming it's in app/build/outputs/apk/release and ends with .apk)
RELEASE_APK=$(find "$ROOT_DIR"/app/build/outputs/apk/release -name "*.apk" -print -quit)
if [[ -z "$RELEASE_APK" ]]; then
  echo "Error: Release APK not found after build."
  exit 1
fi

# Copy the new APK to the download directory with a specific name
NEW_APK_NAME="snap-tiles-v${VERSION_NAME}.apk"
cp "$RELEASE_APK" "$ROOT_DIR"/download/"$NEW_APK_NAME"
echo "==> Copied $NEW_APK_NAME to download/directory."

TAG="v${VERSION_NAME}"
COMMIT_MSG="Release v${VERSION_NAME} (build ${VERSION_CODE})"

echo "==> Version: $VERSION_NAME (code $VERSION_CODE)"
echo "==> Tag:     $TAG"
echo "==> Message: $COMMIT_MSG"
echo ""

# Check for uncommitted changes
if [[ -z "$(git -C "$ROOT_DIR" status --porcelain)" ]]; then
  echo "No changes to commit. Tagging current HEAD."
else
  echo "Staging and committing all changes..."
  git -C "$ROOT_DIR" add -A
  git -C "$ROOT_DIR" commit -m "$COMMIT_MSG"
fi

# Check if tag already exists
if git -C "$ROOT_DIR" rev-parse "$TAG" >/dev/null 2>&1; then
  echo "Error: Tag $TAG already exists. Bump the version first."
  exit 1
fi

git -C "$ROOT_DIR" tag "$TAG"
echo "Created tag $TAG"

git -C "$ROOT_DIR" push origin main --tags
echo ""
echo "==> Released $TAG successfully!"