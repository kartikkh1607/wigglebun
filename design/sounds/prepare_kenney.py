"""Turns Kenney "Interface Sounds" (CC0, https://kenney.nl/assets/interface-sounds) into res/raw files.

kenney.nl refuses scripted downloads, so download and unzip the pack yourself, then:
  python design/sounds/prepare_kenney.py <unzipped dir> tap_soft=<file> toggle=<file> break_start=<file> success=<file>
e.g. ... tap_soft=Audio/click_001.ogg toggle=Audio/toggle_001.ogg
Each file becomes mono, 44.1 kHz, leading silence trimmed, loudness matched to generate_sounds.py.
"""
import pathlib, sys
import numpy as np
import soundfile as sf

SR = 44100
RAW = pathlib.Path(__file__).parent.parent.parent / "app" / "src" / "main" / "res" / "raw"
TARGET_RMS, PEAK = 0.11, 0.85


def load(path):
    y, sr = sf.read(path, always_2d=True)
    y = y.mean(axis=1)
    if sr != SR:
        y = np.interp(np.arange(int(len(y) * SR / sr)) / SR, np.arange(len(y)) / sr, y)
    loud = np.where(np.abs(y) > 0.01)[0]
    return y[loud[0]:] if len(loud) else y  # trim leading silence


def main(argv):
    src = pathlib.Path(argv[0])
    for pair in argv[1:]:
        name, rel = pair.split("=", 1)
        y = load(src / rel)
        y = y * (TARGET_RMS / (np.sqrt(np.mean(y ** 2)) + 1e-9))
        y *= min(1.0, PEAK / max(np.max(np.abs(y)), 1e-9))
        sf.write(RAW / f"{name}.ogg", y.astype(np.float32), SR, format="OGG", subtype="VORBIS")
        print("wrote", name, "from", rel)


if __name__ == "__main__":
    main(sys.argv[1:])
