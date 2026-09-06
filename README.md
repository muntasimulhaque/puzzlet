# Puzzlet

A calm jigsaw puzzle game for ages 3 to 5. One child, one picture, pieces
that click home. Native Android, paid once, fully offline: no ads, no
trackers, no accounts, no network.

Status: release 0.9 (versionCode 9) is cut for the closed testing
track. Four pictures, each growing 4 to 6 to 9 pieces with wins. The goal
always shows faintly on the board and a held piece lights its home; no
difficulty screen, no peek coin, no restart button, no sound switch, no
saved picture. The menu is pictures with their names, and tapping one
plays at once. Two synthesized sounds (a click and a bell, no music),
and wins per picture that survive process death. Store screenshots render
in CI from the app's own states; the listing kit, the console answers and
the signing flow are ready. See AGENTS.md for the working rules and the
decision log.

- **Play Store package:** `io.github.muntasimulhaque.puzzlet`
- **License:** MIT
- **Privacy policy:** [online](https://muntasimulhaque.github.io/puzzlet/privacy.html) · [in this repo](docs/privacy.html)
- **Signed build for the closed testing upload:** `play-store/aab/app-release.aab`

## The game

Four pictures: sailboat, house, balloon, fruit. All inanimate, no faces,
no eyes, drawn as vectors in code. Tapping a picture starts it at once:
4 pieces first, wins grow it to 6, then 9. No timer, no score, no
fail state, no reading required: drag a piece anywhere near its place and
it clicks home with a spring, a soft knock and a haptic tick.

## Build

`JAVA_HOME` must point at the Android Studio JBR (it is not on PATH):

```
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew :app:testReleaseUnitTest :app:lintRelease
./gradlew :tools:test :tools:checkIcons :tools:checkSounds
./gradlew :app:assembleRelease
```

Asset generators (run only after a deliberate design change, then commit
the regenerated files):

```
./gradlew :tools:makeIcons :tools:makeSounds :tools:makeArt
```

## Play Store

The listing kit lives in `play-store/`: the submission guide with the
paste-ready listing and console answers, the feature graphic, the store
icon, and screenshots per form factor (`phone/`, `tablet7/`, `tablet10/`)
captured by CI from the app's own states. The signed AAB lands in
`play-store/aab/` and is emptied after submission.
