# Puzzlet

A calm jigsaw puzzle game for ages 3 to 5. One child, one picture, pieces
that click home. Native Android, paid once, fully offline: no ads, no
trackers, no accounts, no network.

Status: release 1.4 (versionCode 14) is submitted to the closed testing
track. Release 1.4 is the seat release: a scale-pivot drawing bug had
every tray piece
and every held piece drawn off its true seat, so pieces now sit exactly
where the rules put them, inside a rounded felt shelf; the held-up
picture rises above every piece instead of hiding under some; the die-cut
lightened to a paper rim with a whisper of a cut line; the speaker mark
was redrawn in the app's own line hand; and the picture on the top coin
fades up rather than popping. Twelve pictures, five piece counts to
choose from (4, 6, 9, 12, 16), a blank board with the finished picture
behind one coin, an even tray grid, and a sound switch on the picture
shelf. No timer, no score, no fail state, no reading required. Two
synthesized sounds (a click and a bell, no music). Wins, the count a
parent picked and the sound switch survive process death; an unfinished
game does not. Store screenshots render in CI from the app's own states;
the listing kit and the console answers are ready. See AGENTS.md for the
working rules and the decision log.

- **Play Store package:** `io.github.muntasimulhaque.puzzlet`
- **License:** MIT
- **Privacy policy:** [online](https://muntasimulhaque.github.io/puzzlet/privacy.html) · [in this repo](docs/privacy.html)
- **Signed build for the closed testing upload:** `play-store/aab/app-release.aab`
  (downloaded there after each push, deleted after submission; the folder
  is gone while nothing awaits upload)

## The game

Twelve pictures: sailboat, house, balloon, fruit, train, castle, rocket,
lighthouse, truck, airplane, flowers, ice cream. All inanimate, no faces,
no eyes, drawn as vectors in code, and every one of them on a graded
ground so no piece ever comes out blank.

The shelf shows the pictures with their names and a row of counts under
each. Tap a picture and it plays at the count that row shows; tap a count
and that choice sticks. Left alone, a win grows a picture from 4 to 6 to
9 pieces. Pieces wait in a tray above a blank board: a touch lifts a
piece, and it clicks home when carried near its place, with a spring, a
soft knock and a haptic tick. Forgotten the picture? The coin in the top
bar holds it up, and tapping anywhere puts it away.

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
screenshots whenever the UI changes, or the version file does, so a
release always pays for a three-emulator capture run.

## Play Store

The listing kit lives in `play-store/`: the submission guide with the
paste-ready listing, release notes and console answers, the feature
graphic, the store icon, and screenshots per form factor (`phone/`,
`tablet7/`, `tablet10/`) captured by CI from the app's own states.

## The mark

The launcher icon is the gather: three wanderers, sky, coral and leaf
green, closing in on the honey home piece with its sockets open, the
moment before the click. Four colours that read as four colours. The
feature graphic carries the same mark on the brand teal, with the name
given the whole right side and nothing behind it. Both are drawn from
code by `:tools:makeIcons` and `:tools:makeArt`, never hand-edited, and
`checkIcons` fails the build if a committed PNG ever drifts.
