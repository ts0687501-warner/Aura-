# AURA — Phase 1

A working talk-to-AURA loop: wake word ("AURA") → speech-to-text → streaming
Claude reply → streaming text-to-speech, with barge-in (interrupt AURA mid-sentence
by speaking again), plus a Compose HUD with an animated AI core.

## What's implemented (Phase 1)
- `core-voice`: wake-word loop (via repeated `SpeechRecognizer` sessions),
  active listening, TTS with barge-in, foreground service for legal
  background mic use.
- `core-brain`: streaming client for the Anthropic Messages API (SSE).
- `core-ui-theme`: holographic blue Compose theme + animated `CoreOrb`.
- `app`: MainActivity + ChatViewModel wiring it all together, chat log UI,
  manual text input as a fallback to voice.

## Not yet built (next phases)
- Phase 2: `core-memory` (Room DB, semantic search over past conversations)
- Phase 3: `core-automation` + `core-phonecontrol` (open apps, toggle wifi,
  battery-based rules, calendar/alarm actions — needs Accessibility/Intent work)
- Phase 4: `core-vision` (CameraX + ML Kit OCR/object detection)
- Phase 5: `core-plugins`, self-improvement loop, polish

## Setup

1. **Get a Claude API key** from console.anthropic.com.
2. **Never hardcode it.** Wire it in via `AuraApplication.onCreate()`:
   ```kotlin
   ApiKeyProvider.setKey(EncryptedSharedPrefsStore(this).getApiKey())
   ```
   For local dev only, you can temporarily call
   `ApiKeyProvider.setKey("sk-ant-...")` — but do not commit that, and do not
   ship a build with a key embedded in the APK (it's trivially extractable).
   The right long-term answer is a settings screen where you paste your own
   key, stored via `EncryptedSharedPreferences`.
3. Open in Android Studio (Koala+ recommended), let Gradle sync, run on a
   physical device (emulator mics are unreliable for speech testing).
4. Grant the RECORD_AUDIO permission when prompted.

## Known Phase-1 limitations (by design, to be addressed next)
- Wake-word detection uses repeated on-device recognition, not a dedicated
  keyword spotter — noticeably more battery drain than the real thing.
  **Recommendation:** swap in Picovoice Porcupine in Phase 2 (interface in
  `VoiceManager` is already isolated so this is a contained change).
- TTS streams sentence-by-sentence via `QUEUE_ADD`, not true low-latency
  audio streaming — acceptable for now, revisit if latency feels off.
- No memory yet — every session starts fresh. That's Phase 2.
- No phone-control actions yet — AURA can only talk, not act. That's Phase 3.

## Next step
Reply with which module to build next (memory, automation, or vision) and
I'll build it completely, matching this module's structure and quality bar.
