# Puzzlet

A calm jigsaw puzzle game for ages 3 to 5. One child, one picture, pieces
that click home. Native Android, paid once, fully offline: no ads, no
trackers, no accounts, no network.

Status: M1. Three pictures cut into 4 to 24 pieces, drag-and-snap gameplay,
ghost board, peek coin, slot glow, ring-burst snap, confetti celebration.
Sound lands in M2. See AGENTS.md for the working rules and the decision log.

- **Play Store package:** `app.puzzlet` (reserved; not yet published)
- **License:** MIT

## Build

`JAVA_HOME` must point at the Android Studio JBR (it is not on PATH):

```
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew :app:testReleaseUnitTest :app:lintRelease
./gradlew :tools:checkIcons
./gradlew :app:assembleRelease
```
