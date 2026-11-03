# Architectural Rules
- UI Framework: Jetpack Compose
- Language: Kotlin
- Pattern: MVVM (StateFlow/SharedFlow in ViewModel, declarative Composables in View)
- Audio: Android TextToSpeech (TTS initialized in ViewModel/Repository, released on cleared)
- Persistence: DataStore<Preferences> for settings, Room DB for high scores
- Documentation: Every class, ViewModel, and complex method must be fully commented.