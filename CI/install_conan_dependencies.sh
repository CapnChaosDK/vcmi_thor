#!/usr/bin/env bash

RELEASE_TAG="2026-09-01"
FILENAME="$1.txz"
DOWNLOAD_URL="https://github.com/vcmi/vcmi-dependencies/releases/download/$RELEASE_TAG/$FILENAME"

downloadedFile="$RUNNER_TEMP/$FILENAME"
archiveDirectory="${CONAN_DEPENDENCY_ARCHIVE_DIR:-$RUNNER_TEMP}"
downloadedFile="$archiveDirectory/$FILENAME"

mkdir -p "$archiveDirectory"

if [[ ! -s "$downloadedFile" ]]; then
	printf 'Downloading Conan dependency archive: %s\n' "$FILENAME"
	curl --fail --location --retry 3 --retry-all-errors \
		--output "$downloadedFile" "$DOWNLOAD_URL"
else
	printf 'Reusing Conan dependency archive: %s\n' "$downloadedFile"
fi

conan cache restore "$downloadedFile"
