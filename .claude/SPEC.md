# N-Back Game Specifications
- App Type: Native Android App (Kotlin + Jetpack Compose)
- Architecture: MVVM (Model-View-ViewModel)
- Screens:
    1. Home Screen: Shows high scores, current settings (n, event duration, events per round), start game.
    2. Game Screen: 3x3 grid (visual stimuli), spoken letters via TextToSpeech (auditory stimuli), match buttons, HUD (event count, score).
    3. Settings Screen (Optional/2-pt): Edit n-value, grid size, audio letters, duration.
- Requirements:
    - Sequence generator with 20-40% match rate.
    - First `n` items guaranteed NOT to match.
    - Ignore match presses before round starts, after round ends, and during first `n` items.
    - Prevent double-pressing during the same stimulus tick.
    - Unit tests for sequence logic, match rate, first-n guarantee, and double-press guard.