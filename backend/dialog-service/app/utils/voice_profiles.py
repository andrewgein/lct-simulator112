from dataclasses import dataclass
import json
from os import getenv
from pathlib import Path
from random import SystemRandom

from app.grpc.com.simulator112.incident.incident_context_pb2 import (
    AGGRESSIVE,
    CALM,
    CONFUSED,
    MAN,
    PANICKED,
    WOMEN,
    WORRIED,
)


SERVICE_ROOT = Path(__file__).resolve().parents[2]
VOICES_ROOT = SERVICE_ROOT / "resources" / "voices"

_EMOTIONAL_STATES = {
    "calm": CALM,
    "worried": WORRIED,
    "panicked": PANICKED,
    "aggressive": AGGRESSIVE,
    "confused": CONFUSED,
}
_CATEGORY_METADATA = {
    "man": ("male", "adult", "man"),
    "woman": ("female", "adult", "woman"),
    "children": ("child", "child", "child"),
}


@dataclass(frozen=True)
class VoiceProfile:
    id: str
    audio_path: Path
    reference_text: str
    gender: str
    age_group: str
    emotional_states: tuple[int, ...]


def _read_manifest(path: Path) -> dict:
    try:
        manifest = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ValueError(f"Invalid voice manifest {path}: {exc}") from exc

    if not isinstance(manifest, dict) or not isinstance(manifest.get("emotions"), dict):
        raise ValueError(f"Voice manifest {path} must contain an 'emotions' object")
    return manifest


def _load_voice_profiles() -> tuple[VoiceProfile, ...]:
    defaults_path = VOICES_ROOT / "manifest.json"
    defaults = _read_manifest(defaults_path)["emotions"]
    missing_defaults = set(_EMOTIONAL_STATES) - set(defaults)
    if missing_defaults:
        raise ValueError(
            f"Voice manifest {defaults_path} is missing emotions: "
            + ", ".join(sorted(missing_defaults))
        )

    profiles = []
    for category, (gender, age_group, id_prefix) in _CATEGORY_METADATA.items():
        category_dir = VOICES_ROOT / category
        for voice_dir in sorted(path for path in category_dir.iterdir() if path.is_dir()):
            manifest_path = voice_dir / "manifest.json"
            manifest = _read_manifest(manifest_path)
            emotions = manifest["emotions"]
            missing = set(_EMOTIONAL_STATES) - set(emotions)
            extra = set(emotions) - set(_EMOTIONAL_STATES)
            if missing or extra:
                details = []
                if missing:
                    details.append("missing: " + ", ".join(sorted(missing)))
                if extra:
                    details.append("unknown: " + ", ".join(sorted(extra)))
                raise ValueError(f"Invalid emotions in {manifest_path} ({'; '.join(details)})")

            for emotion, emotional_state in _EMOTIONAL_STATES.items():
                config = emotions[emotion]
                default_config = defaults[emotion]
                if not isinstance(config, dict) or not isinstance(default_config, dict):
                    raise ValueError(
                        f"Emotion '{emotion}' in {manifest_path} must be an object"
                    )

                audio_name = config.get("ref_audio")
                if (
                    not isinstance(audio_name, str)
                    or not audio_name
                    or Path(audio_name).name != audio_name
                ):
                    raise ValueError(
                        f"Emotion '{emotion}' in {manifest_path} has an invalid ref_audio"
                    )
                audio_path = voice_dir / audio_name
                if not audio_path.is_file():
                    raise FileNotFoundError(
                        f"Emotion '{emotion}' in {manifest_path} references missing "
                        f"audio file {audio_name}"
                    )

                reference_text = config.get("ref_text", default_config.get("ref_text"))
                if not isinstance(reference_text, str) or not reference_text.strip():
                    raise ValueError(
                        f"Emotion '{emotion}' in {manifest_path} has no ref_text "
                        "and no valid root default"
                    )

                profiles.append(
                    VoiceProfile(
                        id=f"{id_prefix}-{voice_dir.name}-{emotion}",
                        audio_path=audio_path,
                        reference_text=reference_text,
                        gender=gender,
                        age_group=age_group,
                        emotional_states=(emotional_state,),
                    )
                )

    if not profiles:
        raise ValueError(f"No voice profiles found under {VOICES_ROOT}")
    return tuple(profiles)


VOICE_PROFILES = _load_voice_profiles()
_PROFILES_BY_ID = {profile.id: profile for profile in VOICE_PROFILES}
_RANDOM = SystemRandom()


def get_voice_profile(profile_id: str) -> VoiceProfile:
    try:
        return _PROFILES_BY_ID[profile_id]
    except KeyError as exc:
        available = ", ".join(sorted(_PROFILES_BY_ID))
        raise ValueError(
            f"Unknown TTS voice profile '{profile_id}'. Available: {available}"
        ) from exc


def validate_voice_profiles() -> None:
    missing = [
        str(profile.audio_path)
        for profile in VOICE_PROFILES
        if not profile.audio_path.is_file()
    ]
    if missing:
        raise FileNotFoundError("Missing TTS voice profiles: " + ", ".join(missing))


def get_voice_gender(age: int, gender: int) -> str:
    if age < 14:
        return "child"
    return {MAN: "male", WOMEN: "female"}[gender]


def select_voice_profile(
    emotional_state: int,
    gender: str | None = None,
    age_group: str | None = None,
) -> VoiceProfile:
    fixed_profile_id = getenv("TTS_VOICE_PROFILE")
    if fixed_profile_id:
        return get_voice_profile(fixed_profile_id)

    candidates = list(VOICE_PROFILES)

    if gender is not None:
        matching_gender = [profile for profile in candidates if profile.gender == gender]
        if matching_gender:
            candidates = matching_gender

    if age_group is not None:
        matching_age = [profile for profile in candidates if profile.age_group == age_group]
        if matching_age:
            candidates = matching_age

    matching_emotion = [
        profile
        for profile in candidates
        if emotional_state in profile.emotional_states
    ]
    if matching_emotion:
        candidates = matching_emotion

    return _RANDOM.choice(candidates)
