================================================================================
HOW TO ACCESS AND DOWNLOAD YOUR AAB & APK FILES
================================================================================

The AI Studio web file explorer automatically hides binary files with extensions
like .aab, .apk, and .jar to prevent the browser code editor from crashing on
50MB-100MB binary files.

Both your Google Play production AAB and APK files are already built and ready!

--------------------------------------------------------------------------------
OPTION 1: DOWNLOAD DIRECTLY FROM THIS FOLDER (RENAMED AS .ZIP)
--------------------------------------------------------------------------------
Inside this `app/src/` directory, you will see:
  • `app-release.zip`      (47 MB - Exact copy of the production .aab)
  • `app-release-apk.zip`  (101 MB - Exact copy of the production .apk)

Steps:
1. Right-click `app-release.zip` in the file tree and select "Download".
2. On your computer, simply rename the file:
   Rename `app-release.zip` -> `app-release.aab`
   (An Android App Bundle is natively a ZIP archive format. Google Play Console
   will accept this file immediately with no modifications).

--------------------------------------------------------------------------------
OPTION 2: USING AI STUDIO "EXPORT AS ZIP"
--------------------------------------------------------------------------------
1. In the AI Studio top bar or settings menu, click "Export as ZIP".
2. Extract the downloaded ZIP on your computer.
3. Navigate to:
   `app/build/outputs/bundle/release/app-release.aab`
   or
   `app/src/app-release.aab`

--------------------------------------------------------------------------------
OPTION 3: USING GITHUB & GITHUB CODESPACES (RECOMMENDED FOR PRODUCTION)
--------------------------------------------------------------------------------
1. Click "Push to GitHub" in the AI Studio menu.
2. In your GitHub repository, click the green "Code" button -> "Codespaces" -> "Create codespace on main".
3. In GitHub Codespaces (which opens full VS Code in your browser):
   - In the left sidebar explorer, navigate to `app/build/outputs/bundle/release/`
   - Right-click `app-release.aab` -> Click "Download..."
   - It will download directly to your PC with no restrictions.
   - If you ever want to rebuild cleanly in Codespaces, run in the terminal:
     ./gradlew bundleRelease
