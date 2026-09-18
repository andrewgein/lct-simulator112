import argparse
import json
from pathlib import Path

import mlx.core as mx
import numpy as np
from mlx_audio.tts.utils import load_model
from scipy.io import wavfile


DEFAULT_MODEL = "mlx-community/Qwen3-TTS-12Hz-1.7B-VoiceDesign-8bit"
MAX_REVIEW_DURATION_SECONDS = 25.0
PREVIOUS_REFERENCE_TEXT = (
    "Алло! Помогите, тут человек упал, он не дышит! Я не знаю, что делать, "
    "приезжайте скорее, пожалуйста!"
)
REFERENCE_TEXT = (
    "Помогите, пожалуйста. Тут человек упал и не дышит. Я не знаю, что "
    "делать. Приезжайте скорее."
)
CHILD_REFERENCE_TEXT = (
    "Пожалуйста, помогите. Мама упала и не отвечает. Я её зову, а она молчит. "
    "Что мне делать?"
)

VOICE_DESIGNS = (
    "A terrified Russian-speaking man around 25. He is genuinely panicking and speaks extremely fast, urgently and impulsively, with almost no pauses. Natural emergency phone call, clear words, no acting or moaning.",
    "A panicked Russian-speaking man around 40 with a natural deep voice. He speaks very rapidly in one urgent rush because a life is in danger. Intense real fear, clipped phrases, minimal pauses, no theatrical delivery.",
    "A frightened Russian-speaking man around 60 with a low slightly rough voice. He is near panic and desperately asks for help, speaking much faster than normal with urgent uneven rhythm and very short pauses. Realistic and intelligible.",
    "A Russian-speaking man around 28 with a light clear tenor voice. He sounds like an ordinary person making a real emergency call, not an actor. Adrenaline makes him speak quickly in short urgent phrases; the fear is obvious but restrained, natural and fully intelligible.",
    "A Russian-speaking man around 55 with a calm low slightly husky voice. He is normally composed, but the emergency has made him frightened and hurried. He speaks fast and tensely while trying to remain coherent. Realistic telephone speech, subtle panic, no dramatic acting.",
    "A 34-year-old Armenian man speaking Russian fluently with a clearly audible but natural Armenian accent. Warm resonant mid-low baritone, energetic consonants and authentic Armenian-influenced Russian melody, never caricatured. This is a real emergency call: he is badly frightened, speaks rapidly and urgently, takes one short involuntary breath between thoughts, and tries hard to stay coherent. The fear comes from tension, rushed timing and slightly unsteady pitch, not from acting, shouting, sobbing or moaning.",
    "A slim Russian-speaking man around 23 with a youthful light tenor, slightly dry and naturally narrow in timbre. He has just witnessed a life-threatening event and is in genuine shock. His speech is fast and spontaneous, the first words come out abruptly, then he takes a small quick breath and continues in short urgent bursts. Subtle pitch instability and tight articulation reveal panic. Intimate realistic phone-call sound, no announcer voice, no theatrical crying and no exaggerated gasping.",
    "A Russian-speaking working-class man around 48 with a broad chest, a deep rough bass-baritone and a faint smoker's rasp. Normally calm and laconic, he is now seriously frightened but fighting to remain useful. He speaks quickly in a low tense voice, compresses phrases, briefly inhales through the nose before asking for help, and occasionally stresses the wrong word under adrenaline. Natural human micro-pauses and imperfect rhythm, fully intelligible, with no performance, melodrama, screaming or nonverbal groans.",
)

WOMAN_VOICE_DESIGNS = (
    "A 23-year-old Russian woman with a bright, clear soprano voice, speaking naturally like an ordinary person. She is frightened and speaks quickly while trying to explain the emergency clearly. Only normal spoken words.",
    "A 38-year-old Russian woman with a low, slightly husky contralto voice and direct everyday pronunciation. She is seriously alarmed and talks fast, firmly and naturally because help is urgent.",
    "A 67-year-old Russian woman with a thin, mature voice and gentle age-related roughness. She is scared and confused, speaking quickly but clearly to the emergency operator. Natural conversational speech.",
    "A 31-year-old Russian woman with a soft, warm mezzo-soprano and relaxed everyday intonation. She is worried but remains composed, speaking clearly at a natural conversational pace.",
    "A 52-year-old Russian woman with a deep, steady alto voice and confident practical diction. She feels anxious but controls herself, giving information calmly and a little faster than usual.",
)

CHILD_VOICE_DESIGNS = (
    "A six-year-old Russian boy with a small bright voice, speaking naturally like a real child. He is frightened and talks quickly while trying to be brave. Clear ordinary speech with no non-speech vocalizations.",
    "An eight-year-old Russian girl with a low, slightly husky child voice. She is genuinely scared and asks for help in quick, spontaneous phrases. Natural everyday speech, not a performance.",
    "An eleven-year-old Russian boy whose voice is beginning to change, with a light rasp and an uneven youthful pitch. He sounds alarmed and speaks fast, directly and naturally.",
    "A seven-year-old Russian girl with a soft, quiet and high child voice. She is worried and unsure but still composed, speaking naturally to a trusted adult at a normal pace.",
    "A thirteen-year-old Russian girl with a calm, low adolescent voice and slightly reserved pronunciation. She is anxious but keeps control, speaking clearly and a little faster than usual.",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate frightened Russian voice candidates with Qwen VoiceDesign."
    )
    parser.add_argument("--count", type=int)
    parser.add_argument("--start-index", type=int, default=1)
    parser.add_argument(
        "--profile",
        choices=("man", "woman", "children"),
        default="man",
    )
    parser.add_argument("--model", default=DEFAULT_MODEL)
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=None,
    )
    parser.add_argument("--seed", type=int, default=212)
    parser.add_argument("--temperature", type=float, default=0.75)
    return parser.parse_args()


def to_int16(audio: np.ndarray) -> np.ndarray:
    audio = np.asarray(audio, dtype=np.float32).reshape(-1)
    return (np.clip(audio, -1.0, 1.0) * 32767.0).astype(np.int16)


def main() -> None:
    args = parse_args()
    profile_config = {
        "man": (VOICE_DESIGNS, REFERENCE_TEXT, "man_voice", "very_frightened_man"),
        "woman": (
            WOMAN_VOICE_DESIGNS,
            REFERENCE_TEXT,
            "woman_voice",
            "woman_voice",
        ),
        "children": (
            CHILD_VOICE_DESIGNS,
            CHILD_REFERENCE_TEXT,
            "children_voice",
            "child_voice",
        ),
    }
    voice_designs, reference_text, output_folder, filename_prefix = profile_config[
        args.profile
    ]
    output_dir = args.output_dir or Path(
        f"resources/voice_candidates/generate_voice/{output_folder}"
    )
    count = args.count or len(voice_designs)

    if not 1 <= args.start_index <= len(voice_designs):
        raise ValueError(
            f"--start-index must be between 1 and {len(voice_designs)}"
        )
    end_index = args.start_index - 1 + count
    if count < 1 or end_index > len(voice_designs):
        raise ValueError("selected sample range is outside available voice designs")

    output_dir.mkdir(parents=True, exist_ok=True)
    model = load_model(model_path=args.model)
    sample_rate = int(model.sample_rate)
    manifest_path = output_dir / "manifest.json"
    existing_samples = {}
    if manifest_path.is_file():
        existing_manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        existing_samples = {
            sample["file"]: sample
            for sample in existing_manifest.get("samples", [])
        }
        for sample in existing_samples.values():
            sample.setdefault("referenceText", PREVIOUS_REFERENCE_TEXT)

    manifest = {
        "model": args.model,
        "referenceText": reference_text,
        "samples": existing_samples,
    }

    selected_designs = voice_designs[args.start_index - 1 : end_index]
    for index, instruction in enumerate(selected_designs, start=args.start_index):
        seed = args.seed + index - 1
        mx.random.seed(seed)
        chunks = [
            np.asarray(result.audio, dtype=np.float32).reshape(-1)
            for result in model.generate(
                text=reference_text,
                instruct=instruction,
                lang_code="russian",
                temperature=args.temperature,
                max_tokens=512,
                stream=False,
            )
        ]
        if not chunks:
            raise RuntimeError(f"VoiceDesign returned no audio for sample {index}")

        audio = np.concatenate(chunks)
        duration_seconds = len(audio) / sample_rate
        filename = f"{filename_prefix}_{index:02d}.wav"
        wavfile.write(output_dir / filename, sample_rate, to_int16(audio))
        review_recommended = duration_seconds <= MAX_REVIEW_DURATION_SECONDS
        manifest["samples"][filename] = {
            "file": filename,
            "seed": seed,
            "instruction": instruction,
            "referenceText": reference_text,
            "durationSeconds": round(duration_seconds, 2),
            "reviewRecommended": review_recommended,
        }
        warning = " (skip: abnormally long)" if not review_recommended else ""
        print(f"Generated {filename}: {duration_seconds:.2f}s{warning}")

    manifest["samples"] = [
        manifest["samples"][filename]
        for filename in sorted(manifest["samples"])
    ]
    manifest_path.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Manifest: {manifest_path}")


if __name__ == "__main__":
    main()
