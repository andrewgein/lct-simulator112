#!/usr/bin/env python3
"""Generate the same text with every configured voice/emotion profile."""

import argparse
import json
from pathlib import Path
import sys
import wave

import numpy as np
from dotenv import load_dotenv

# Allow running this file directly from the repository root or scripts/ directory.
SERVICE_ROOT = Path(__file__).resolve().parents[1]
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

from app.utils.tts_model import OUTPUT_SAMPLE_RATE, TTSError, TTSModel
from app.utils.voice_profiles import VOICE_PROFILES, VOICES_ROOT, VoiceProfile


DEFAULT_OUTPUT_DIR = SERVICE_ROOT / "resources" / "generated_voice_samples"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Synthesize one text with every voice/emotion profile through "
            "the configured qwentts.cpp server. Existing files are skipped."
        )
    )
    text_source = parser.add_mutually_exclusive_group(required=True)
    text_source.add_argument("--text", help="text to synthesize")
    text_source.add_argument(
        "--text-file",
        type=Path,
        help="UTF-8 file containing the text to synthesize",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=DEFAULT_OUTPUT_DIR,
        help=f"output directory (default: {DEFAULT_OUTPUT_DIR})",
    )
    parser.add_argument(
        "--profile",
        action="append",
        choices=sorted(profile.id for profile in VOICE_PROFILES),
        help="generate only this profile; may be supplied more than once",
    )
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help="replace existing WAV files instead of skipping them",
    )
    parser.add_argument(
        "--fail-fast",
        action="store_true",
        help="stop after the first synthesis error",
    )
    return parser.parse_args()


def read_text(args: argparse.Namespace) -> str:
    text = args.text
    if args.text_file is not None:
        text = args.text_file.read_text(encoding="utf-8")
    text = text.strip()
    if not text:
        raise ValueError("synthesis text must not be empty")
    return text


def output_path_for(profile: VoiceProfile, output_dir: Path) -> Path:
    voice_dir = profile.audio_path.parent.relative_to(VOICES_ROOT)
    emotion = profile.id.rsplit("-", 1)[-1]
    return output_dir / voice_dir / f"{emotion}.wav"


def float_to_pcm16(audio: np.ndarray) -> bytes:
    samples = np.asarray(audio, dtype=np.float32).reshape(-1)
    return (np.clip(samples, -1.0, 1.0) * 32767.0).astype("<i2").tobytes()


def generate_sample(
    model: TTSModel,
    profile: VoiceProfile,
    text: str,
    output_path: Path,
) -> int:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    temporary_path = output_path.with_suffix(output_path.suffix + ".part")
    temporary_path.unlink(missing_ok=True)
    sample_count = 0

    try:
        with wave.open(str(temporary_path), "wb") as output:
            output.setnchannels(1)
            output.setsampwidth(2)
            output.setframerate(OUTPUT_SAMPLE_RATE)
            for chunk in model.generate(text=text, profile=profile):
                if chunk.sample_rate != OUTPUT_SAMPLE_RATE:
                    raise TTSError(
                        f"Unexpected sample rate {chunk.sample_rate}; "
                        f"expected {OUTPUT_SAMPLE_RATE}"
                    )
                samples = np.asarray(chunk.audio).reshape(-1)
                if samples.size:
                    output.writeframes(float_to_pcm16(samples))
                    sample_count += int(samples.size)

        if sample_count == 0:
            raise TTSError("qwentts.cpp returned no audio")
        temporary_path.replace(output_path)
        return sample_count
    except Exception:
        temporary_path.unlink(missing_ok=True)
        raise


def main() -> int:
    load_dotenv(SERVICE_ROOT / ".env")
    args = parse_args()
    try:
        text = read_text(args)
    except (OSError, UnicodeError, ValueError) as exc:
        print(f"Error: {exc}", file=sys.stderr)
        return 2

    selected_ids = set(args.profile or ())
    profiles = [
        profile
        for profile in VOICE_PROFILES
        if not selected_ids or profile.id in selected_ids
    ]
    model = TTSModel()
    if not model.base_url:
        print("Error: QWENTS_BASE_URL is not configured", file=sys.stderr)
        return 2

    generated = []
    skipped = []
    failed = []
    total = len(profiles)

    for index, profile in enumerate(profiles, start=1):
        output_path = output_path_for(profile, args.output_dir)
        relative_output = output_path.relative_to(args.output_dir)
        if output_path.exists() and not args.overwrite:
            print(f"[{index}/{total}] Skip {profile.id}: {relative_output}")
            skipped.append(profile.id)
            continue

        print(f"[{index}/{total}] Generate {profile.id} -> {relative_output}")
        try:
            sample_count = generate_sample(model, profile, text, output_path)
        except (TTSError, OSError, ValueError) as exc:
            print(f"  Failed: {exc}", file=sys.stderr)
            failed.append({"profile": profile.id, "error": str(exc)})
            if args.fail_fast:
                break
            continue

        duration = sample_count / OUTPUT_SAMPLE_RATE
        print(f"  Done: {duration:.2f}s")
        generated.append(profile.id)

    args.output_dir.mkdir(parents=True, exist_ok=True)
    report_path = args.output_dir / "generation-report.json"
    report_path.write_text(
        json.dumps(
            {
                "text": text,
                "sampleRate": OUTPUT_SAMPLE_RATE,
                "generated": generated,
                "skipped": skipped,
                "failed": failed,
            },
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    print(
        f"Finished: {len(generated)} generated, {len(skipped)} skipped, "
        f"{len(failed)} failed. Report: {report_path}"
    )
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
