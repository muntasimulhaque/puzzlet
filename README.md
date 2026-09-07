# Puzzlet

A calm jigsaw puzzle game for ages 3 to 5. One child, one picture, pieces
that click home. Native Android, paid once, fully offline: no ads, no
trackers, no accounts, no network.

Status: release 1.6 (versionCode 16) is submitted to the closed testing
track. Release 1.6 is the complete shelf: all twelve pictures are back, each
opening a cut chooser whose five tiles show the real cut the game will
deal at 4, 6, 9, 12 and 16 pieces; the child picks a puzzle with their
eyes, and the pick sticks. The finish now breathes: one quiet second on
the completed table before the celebration plate rises. The gather mark
is drawn tighter, with every knob hovering a hair outside its open
socket, and the feature graphic follows at a larger mark and type. No
timer, no score, no fail state, no reading required. Three synthesized
sounds (a tap, a click and a bell, no music). Wins, the count the child
picked and the sound switch survive process death; an unfinished game
does not. Store screenshots render in CI from the app's own states; the
listing kit and the console answers are ready. See AGENTS.md for the
working rules and the decision log.

- **Play Store package:** `io.github.muntasimulhaque.puzzlet`
- **License:** MIT
- **Privacy policy:** [online](https://muntasimulhaque.github.io/puzzlet/privacy.html) · [in this repo](docs/privacy.html)
- **Signed build for the closed testing upload:** `play-store/aab/app-release.aab`
  (downloaded there after each push, deleted after submission; the folder
  is gone while nothing awaits upload)

## The game

Twelve pictures: sailboat, house, balloon, fruit, lighthouse, ice cream,
train, castle, rocket, truck, airplane, flowers. All inanimate, no
faces, no eyes, drawn as vectors in code, and every one of them on a
graded ground so no piece ever comes out blank.

Tap a picture and its cut chooser rises: five tiles, each the picture
cut the real way the game will deal it at 4, 6, 9, 12 or 16 pieces, the
count each opens at marked in honey. A three-year-old picks by look;
the numeral under each tile speaks to the parent. Tapping a tile plays
at once and remembers it; where nobody has picked, a win grows a
picture from 4 to 6 to 9 pieces. Pieces wait in a tray above a blank
board: a touch lifts a piece with a soft tap, and it clicks home when
carried near its place, with a spring, a soft knock and a haptic tick.
Forgotten the picture? The coin in the top bar holds it up, and tapping
anywhere puts it away. Leaving a started picture asks first. Finish,
and the table holds the completed picture for one quiet second before
the celebration plate rises with confetti, Again leading.

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
green, closing in on the honey home piece whose sockets sit open, every
knob hovering a hair outside its socket: the moment before the click.
Four colors that read as four colors. The feature graphic carries the
same mark on the brand teal, with the name given the whole right side
and nothing behind it. Both are drawn from code by `:tools:makeIcons`
and `:tools:makeArt`, never hand-edited, and `checkIcons` fails the
build if a committed PNG ever drifts.
