# Publishing a GitHub Release

Read this when the user wants the release on GitHub, or when you offer it in step 8. The repo is `RakinRkz/Audio-Quran` (`origin`). A GitHub Release is how people who don't use the Play Store get the app: they download the APK from the release page and install it.

Everything here is visible to anyone who can see the repo, and a published release notifies watchers. Do it only after the user says yes in this conversation, and offer a draft as the safe default.

## Before creating it

1. `gh auth status` must show a logged-in account with access to the repo. If it doesn't, tell the user to run `gh auth login` themselves and stop.
2. The release commit and tag must be on GitHub first:
   ```bash
   git push origin HEAD
   git push origin vX.Y.Z
   ```
   This matters because `gh release create` with a tag that GitHub doesn't know about **creates that tag itself on the remote's default branch**, which may not be the commit you built. `--verify-tag` below makes it fail instead.
   If the release commit is on a branch other than `main`, say so before pushing. The usual flow here is to merge to `main` first, so the tag sits on `main`'s history.
3. Check that the APK in `dist/vX.Y.Z/` is the signed one (step 5 passed). Never attach an unsigned or debug-signed APK, because users can't install it.

## Create it

Attach the APK and a checksum file so people can confirm their download. Leave out the `.aab` (only the Play Store can use it) and `mapping.txt` (private: it reveals the unobfuscated code structure).

```bash
cd dist/vX.Y.Z
sha256sum audioquran-vX.Y.Z.apk > audioquran-vX.Y.Z.apk.sha256
cd -
gh release create vX.Y.Z \
  "dist/vX.Y.Z/audioquran-vX.Y.Z.apk#Audio Quran vX.Y.Z (APK)" \
  "dist/vX.Y.Z/audioquran-vX.Y.Z.apk.sha256" \
  --verify-tag --draft \
  --title "Audio Quran vX.Y.Z" \
  --notes-file <notes file> --generate-notes
```

- `--draft` creates the release unpublished, so the user can read it on GitHub and press Publish. Drop it only when the user asked to publish directly.
- `--generate-notes` appends GitHub's list of merged PRs since the previous release. The notes file goes first. Write it to the scratchpad, not the repo, and keep it short:

  ```markdown
  ## Install
  Download `audioquran-vX.Y.Z.apk` below and open it on your Android phone (Android 8.0+).
  If an older version from a different source is installed (for example a debug build or the Play Store version), uninstall it first. Android refuses updates signed with a different key. Uninstalling clears the saved audio folder, so you'll pick it again.

  SHA-256 of the APK: `<value from the .sha256 file>`

  ## Changes
  <2-5 user-facing bullets from `git log <previous tag>..vX.Y.Z --oneline`; skip if there is no previous tag>
  ```
- Add `--prerelease` for a test or beta build.

## Afterwards

Report the release URL that `gh` prints, and say whether it's a draft. To publish a draft later: `gh release edit vX.Y.Z --draft=false`. To fix an asset: `gh release upload vX.Y.Z <file> --clobber`.
