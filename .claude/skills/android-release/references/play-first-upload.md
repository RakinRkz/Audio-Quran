# First upload to the Play Store

Read this when the release is going to Play for the first time. Put the points that apply into the final summary as a short checklist. None of them block building the release, and the build doesn't need to change for them, except the target SDK in point 1 if Play's minimum is above what the app targets.

Play's rules change over time. Where a point below depends on a current rule, tell the user to check it in Play Console or the linked page rather than stating it as fact.

1. **Target SDK.** Play only accepts new apps and updates that target a recent Android API level, and the minimum goes up every year around August. It was API 35 from August 2025, and later deadlines raise it further. This app targets 34 (`targetSdk` in `app/build.gradle.kts`), so it is very likely too low. Current requirement: https://developer.android.com/google/play/requirements/target-sdk. Raising it is a separate change, not part of the release. It means raising `compileSdk` and possibly AGP, and from API 35 on apps always draw edge-to-edge, so the UI needs checking on a device. Say so, and offer to do it as its own task.
2. **The package name is permanent.** After the first upload, `applicationId` (`com.audioquran`) can never change for that Play listing, and it must not already be taken on Play. If the user wants something like `com.rakinrkz.audioquran`, it has to change before the first upload.
3. **Play App Signing and sideloaded copies.** New apps use Play App Signing: the user's key is the *upload* key, and Google re-signs the app with its own key. So the APK from a GitHub Release (signed with the upload key) and the Play Store install have different signatures. Android won't update one with the other; switching means uninstalling first, which wipes the saved folder. Mention this when the release goes to both GitHub and Play.
4. **Play Console forms this app needs.** These depend on what's in the manifest:
   - the foreground service declaration for `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (background playback);
   - the Data safety form (the app reads audio files the user picks, and stores the folder choice on the device);
   - a privacy policy URL;
   - the content rating questionnaire and the target audience.
5. **Testing before production.** Personal developer accounts created after November 2023 must run a closed test with a minimum number of testers for a set period before production access is granted (it was 12 testers for 14 days). Point the user to Play Console for their account's current requirement.
You don't need to upload `mapping.txt` separately: the `.aab` already contains it, and Play uses it to deobfuscate crash reports.
