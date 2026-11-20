#!/usr/bin/env bash
#
# convert_fastlane_locale_folders.sh
#
# Renames subdirectories named using Android's locale-folder convention
# (en-rUS, pt-rBR, zh-rCN, ...) to the convention expected by fastlane
# (en-US, pt-BR, zh-CN, ...).
#
# Usage:
#   ./convert_fastlane_locale_folders.sh [-n] [-v] <target_directory>
#
# Options:
#   -n    Dry run. Show what would be renamed without changing anything.
#   -v    Verbose. Print every folder examined, not just renamed ones.
#   -h    Show this help text and exit.
#
# Examples:
#   ./convert_fastlane_locale_folders.sh fastlane/metadata/android
#   ./convert_fastlane_locale_folders.sh -n -v ./locales
#
set -euo pipefail
IFS=$'\n\t'
shopt -s nullglob

SCRIPT_NAME="$(basename "$0")"
DRY_RUN=0
VERBOSE=0
TARGET_DIR=""

# Files every Android-style locale folder must contain. Folders missing
# any of these are deleted instead of renamed.
REQUIRED_FILES=(title.txt short_description.txt long_description.txt)

# print_usage: Print help text to stdout.
print_usage() {
  cat <<EOF
Usage: ${SCRIPT_NAME} [-n] [-v] <target_directory>

Renames subdirectories of <target_directory> from the Android locale-folder
naming convention (en-rUS, pt-rBR, zh-rCN, ...) to the convention expected
by fastlane (en-US, pt-BR, zh-CN, ...). Folders already in fastlane format,
or that don't look like locale folders, are left untouched.

Android-style locale folders that are empty, or that are missing any of
title.txt, short_description.txt or long_description.txt, are deleted.
If a folder with the fastlane name already exists, it is deleted and
replaced.

Options:
  -n    Dry run: show what would happen, make no changes.
  -v    Verbose: print every directory examined, not just renamed ones.
  -h    Show this help text and exit.
EOF
}

# log: Print a message to stderr. Only printed when verbose mode is on,
# unless the second argument is the literal string "force".
#
# Arguments:
#   $1 - message to print
#   $2 - (optional) "force" to print regardless of verbosity
log() {
  local message="$1"
  local force="${2:-}"
  if [[ "${VERBOSE}" -eq 1 || "${force}" == "force" ]]; then
    echo "${message}" >&2
  fi
}

# die: Print an error message to stderr and exit with a non-zero status.
#
# Arguments:
#   $1 - error message
die() {
  echo "Error: $1" >&2
  exit 1
}

# android_locale_to_fastlane: Convert one Android-style locale string to
# the fastlane equivalent.
#
# Android marks the region subtag with a leading "r" (e.g. "en-rUS").
# fastlane, like standard BCP 47, drops the "r" (e.g. "en-US").
# Strings that don't match the Android pattern (already-fastlane-style
# folders such as "en", "en-US", or non-locale folders) are echoed back
# unchanged so callers can detect a no-op conversion.
#
# Arguments:
#   $1 - locale string to convert (e.g. "en-rUS")
#
# Outputs:
#   Writes the converted locale string to stdout.
android_locale_to_fastlane() {
  local input="$1"
  local output

  if [[ "${input}" =~ ^([a-zA-Z]{2,3})-r([A-Za-z0-9]{2,3})$ ]]; then
    output="${BASH_REMATCH[1]}-${BASH_REMATCH[2]^^}"
  else
    output="${input}"
  fi

  printf '%s' "${output}"
}

# convert_locale_folders: Walk every immediate subdirectory of a target
# directory and rename any Android-style locale folders to fastlane-style
# names. Skips folders that are already correctly named. Android-style
# folders that are empty or missing any file in REQUIRED_FILES are
# deleted. If the destination name already exists, the existing folder is
# deleted first and then replaced.
#
# Arguments:
#   $1 - path to the directory containing locale subfolders
convert_locale_folders() {
  local target_dir="$1"
  local entry base_name new_name new_path required missing
  local renamed_count=0
  local skipped_count=0
  local removed_count=0
  local incomplete_count=0

  for entry in "${target_dir}"/*/; do
    entry="${entry%/}"
    base_name="$(basename "${entry}")"
    new_name="$(android_locale_to_fastlane "${base_name}")"

    if [[ "${new_name}" == "${base_name}" ]]; then
      log "Skipping '${base_name}' (already fastlane format or not a locale folder)"
      skipped_count=$((skipped_count + 1))
      continue
    fi

    # Empty source folder: nothing to move, so just delete it.
    if [[ -z "$(ls -A -- "${entry}")" ]]; then
      if [[ "${DRY_RUN}" -eq 1 ]]; then
        log "[dry-run] '${base_name}' is empty, would delete it" force
      else
        rmdir -- "${entry}"
        log "'${base_name}' is empty, deleted it" force
      fi
      removed_count=$((removed_count + 1))
      continue
    fi

    # Incomplete source folder: delete it instead of renaming it.
    missing=""
    for required in "${REQUIRED_FILES[@]}"; do
      if [[ ! -f "${entry}/${required}" ]]; then
        missing="${missing:+${missing}, }${required}"
      fi
    done

    if [[ -n "${missing}" ]]; then
      if [[ "${DRY_RUN}" -eq 1 ]]; then
        log "[dry-run] '${base_name}' is missing ${missing}, would delete it" force
      else
        rm -rf -- "${entry}"
        log "'${base_name}' is missing ${missing}, deleted it" force
      fi
      incomplete_count=$((incomplete_count + 1))
      continue
    fi

    new_path="${target_dir}/${new_name}"

    if [[ "${DRY_RUN}" -eq 1 ]]; then
      if [[ -e "${new_path}" ]]; then
        log "[dry-run] '${new_name}' already exists, would delete it" force
      fi
      log "[dry-run] Would rename '${base_name}' -> '${new_name}'" force
    else
      if [[ -e "${new_path}" ]]; then
        log "Warning: '${new_name}' already exists, deleting it before rename" force
        rm -rf -- "${new_path}"
      fi
      mv -- "${entry}" "${new_path}"
      log "Renamed '${base_name}' -> '${new_name}'" force
    fi
    renamed_count=$((renamed_count + 1))
  done

  log "Done. ${renamed_count} folder(s) renamed, ${removed_count} empty folder(s) deleted, ${incomplete_count} incomplete folder(s) deleted, ${skipped_count} skipped." force
}

# parse_args: Parse command-line flags and the required target directory
# argument, populating the globals DRY_RUN, VERBOSE, and TARGET_DIR.
#
# Arguments:
#   "$@" - the script's command-line arguments
parse_args() {
  while getopts ":nvh" opt; do
    case "${opt}" in
      n) DRY_RUN=1 ;;
      v) VERBOSE=1 ;;
      h) print_usage; exit 0 ;;
      \?) die "Unknown option: -${OPTARG}" ;;
    esac
  done
  shift $((OPTIND - 1))

  if [[ $# -lt 1 ]]; then
    print_usage
    die "Missing required <target_directory> argument."
  fi
  TARGET_DIR="$1"
}

main() {
  parse_args "$@"

  [[ -d "${TARGET_DIR}" ]] || die "'${TARGET_DIR}' is not a directory or does not exist."

  convert_locale_folders "${TARGET_DIR}"
}

main "$@"