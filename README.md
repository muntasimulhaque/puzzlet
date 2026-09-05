# Puzzlet

A calm jigsaw puzzle game for ages 3 to 5. One child, one picture, pieces
that click home. Native Android, paid once, fully offline: no ads, no
trackers, no accounts, no network.

Status: release 0.2 (versionCode 2) is cut and signed for the closed
testing track. Eight pictures, each cut into 4 to 24 pieces: drag-and-snap
with a forgiving touch, the faint ghost board, a peek coin, confetti at
the finish, four synthesized sounds (no music) behind a sound switch, and
one unfinished picture that survives process death. Store screenshots
render in CI from the app's own states; the listing kit, the console
answers and the signing flow are ready. See AGENTS.md for the working
rules and the decision log.

- **Play Store package:** `io.github.muntasimulhaque.puzzlet`
- **License:** MIT
- **Privacy policy:** [online](https://muntasimulhaque.github.io/puzzlet/privacy.html) · [in this repo](docs/privacy.html)
- **Signed build for the closed testing upload:** `play-store/aab/app-release.aab`

## The game

Eight pictures: sailboat, rocket, house, lighthouse, balloon, train,
castle, fruit. All inanimate, no faces, no eyes, drawn as vectors in code.
Each cuts into 4, 6, 9, 12, 16, 20 or 24 pieces. No timer, no score, no
fail state, no reading required: drag a piece anywhere near its place and
it clicks home.

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
