# Puzzlet

A calm jigsaw puzzle game for ages 3 to 5. One child, one picture, pieces
that click home. Native Android, paid once, fully offline: no ads, no
trackers, no accounts, no network.

Status: release 1.1 (versionCode 11) is submitted to the closed testing
track. Twelve pictures, five piece counts to choose from (4, 6, 9, 12,
16), a blank board with the finished picture behind one coin, an even
tray grid, and a sound switch on the picture shelf. No timer, no score,
no fail state, no reading required. Two synthesized sounds (a click and a
bell, no music). Wins, the count a parent picked and the sound switch
survive process death; an unfinished game does not. Store screenshots
render in CI from the app's own states; the listing kit and the console
answers are ready. See AGENTS.md for the working rules and the decision
log.

- **Play Store package:** `io.github.muntasimulhaque.puzzlet`
- **License:** MIT
- **Privacy policy:** [online](https://muntasimulhaque.github.io/puzzlet/privacy.html) · [in this repo](docs/privacy.html)
- **Signed build for the closed testing upload:** `play-store/aab/app-release.aab`
  (downloaded there after each push, emptied after submission)

## The game

Twelve pictures: sailboat, house, balloon, fruit, train, castle, rocket,
lighthouse, truck, airplane, flowers, ice cream. All inanimate, no faces,
no eyes, drawn as vectors in code, and every one of them on a graded
ground so no piece ever comes out blank.

The shelf shows the pictures with their names and a row of counts under
each. Tap a picture and it plays at the count that row shows; tap a count
and that choice sticks. Left alone, a win grows a picture from 4 to 6 to
9 pieces. Pieces wait in a tray above a blank board: drag one anywhere
near its place and it clicks home with a spring, a soft knock and a
haptic tick. Forgotten the picture? The coin in the top bar holds it up,
and tapping anywhere puts it away.

## Layout

```
core/     its own Gradle module, pure Kotlin, zero Android imports: cut,
          scenes, ladder, board and tray rules
host/     ViewModel: which screen, which piece in hand, wins, sound
ui/       Compose: picture shelf, play field, celebration
theme     PuzzletColors + Baloo 2 typography; icons drawn as geometry
tools/    offline asset generators: plain JVM Kotlin, Java2D, no libraries
```

The rules are pure data and functions; Android is a player of those
rules, not a participant. No composable takes a ViewModel, which is what
lets the screenshot harness host every state with no-op callbacks.

## Build

`JAVA_HOME` must point at the Android Studio JBR (it is not on PATH):

```
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew :core:test :app:testReleaseUnitTest :app:lintRelease
./gradlew :tools:test :tools:checkIcons :tools:checkSounds
./gradlew :app:assembleRelease
```

Asset generators (run only after a deliberate design change, then commit
the regenerated files):

```
./gradlew :tools:makeIcons :tools:makeSounds :tools:makeArt
```

`:tools:makeScenes` draws every picture into `build/scenes` for review.
It is scratch, never committed: the repo keeps no candidates folder.

CI is the loop: `build.yml` gates every push to `main` on the tests,
lint and the asset pins, then signs and publishes the AAB and APK to the
`latest-build` release. `screenshots.yml` recaptures the store
screenshots whenever the UI changes.

## Play Store

The listing kit lives in `play-store/`: the submission guide with the
paste-ready listing, release notes and console answers, the feature
graphic, the store icon, and screenshots per form factor (`phone/`,
`tablet7/`, `tablet10/`) captured by CI from the app's own states.
