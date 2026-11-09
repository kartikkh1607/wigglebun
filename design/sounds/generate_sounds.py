"""Synthesises Wigglebun's own sounds (mono, 44.1 kHz, OGG Vorbis). Run: python design/sounds/generate_sounds.py

Writes three candidate styles (A, B, C) of bun_chirp (x3 pitches), celebrate and sparks to
design/sounds/candidates/ for listening, and copies style A plus the placeholder UI sounds to
app/src/main/res/raw/. To switch style, copy the chosen candidate over the res/raw file.
The UI sounds (tap_soft, toggle, break_start, success) are placeholders until Kenney's CC0
"Interface Sounds" are put through prepare_kenney.py (see README in this folder).
Needs: numpy, soundfile (pip install soundfile).
"""
import pathlib
import numpy as np
import soundfile as sf

SR = 44100
HERE = pathlib.Path(__file__).parent
CAND = HERE / "candidates"
RAW = HERE.parent.parent / "app" / "src" / "main" / "res" / "raw"
TARGET_RMS = 0.11   # loudness matched across files
PEAK = 0.85


def t(sec):
    return np.arange(int(SR * sec)) / SR


def note(freq, dur, attack=0.015, decay=6.0, harm=(1.0, 0.25, 0.08), glide=0.0):
    """Soft tone: sine + quiet low harmonics only (no harsh highs), exponential decay."""
    x = t(dur)
    f = freq * (1 + glide * x / dur)
    phase = 2 * np.pi * np.cumsum(f) / SR
    y = sum(a * np.sin((i + 1) * phase) for i, a in enumerate(harm))
    env = np.minimum(1, x / attack) * np.exp(-decay * x)
    return y * env


def mix(parts, total):
    out = np.zeros(int(SR * total))
    for start, y in parts:
        i = int(SR * start)
        out[i:i + len(y)] += y[: len(out) - i]
    return out


def lowpass(y, cutoff=5000):
    # one-pole smoothing keeps highs gentle
    a = np.exp(-2 * np.pi * cutoff / SR)
    out = np.empty_like(y)
    s = 0.0
    for i, v in enumerate(y):
        s = (1 - a) * v + a * s
        out[i] = s
    return out


def chirp(base, style):
    # warm two-note rising squeak, 250-400 ms, soft attack
    if style == "A":
        n1, n2, d1, d2 = base, base * 1.26, 0.13, 0.20
    elif style == "B":
        n1, n2, d1, d2 = base, base * 1.5, 0.11, 0.22
    else:
        n1, n2, d1, d2 = base * 0.94, base * 1.12, 0.14, 0.2
    return mix([(0, note(n1, d1 + 0.05, glide=0.05, decay=8)),
                (d1, note(n2, d2 + 0.05, glide=0.12, decay=7))], d1 + d2 + 0.08)


def bells(freqs, gap, dur=0.5, decay=5.5):
    parts = [(i * gap, note(f, dur, attack=0.004, decay=decay, harm=(1.0, 0.0, 0.18))) for i, f in enumerate(freqs)]
    return mix(parts, gap * (len(freqs) - 1) + dur)


def celebrate(style):
    base = {"A": 520, "B": 440, "C": 600}[style]
    c = chirp(base, "A")
    ratios = {"A": (1.5, 1.89, 2.52), "B": (1.26, 1.5, 2.0), "C": (1.33, 1.78, 2.38)}[style]
    b = bells([base * r for r in ratios], 0.17)
    return mix([(0, c), (0.3, b)], 1.1)


def sparks(style):
    f = {"A": 1568, "B": 1318, "C": 1760}[style]
    return bells([f, f * 1.25, f * 1.5], 0.07, dur=0.28, decay=9)


def finish(y):
    y = lowpass(y, 6000)
    fade = int(SR * 0.01)
    y[-fade:] *= np.linspace(1, 0, fade)
    y = y * (TARGET_RMS / (np.sqrt(np.mean(y ** 2)) + 1e-9))
    peak = np.max(np.abs(y))
    if peak > PEAK:
        y *= PEAK / peak
    return y.astype(np.float32)


def save(y, path):
    sf.write(path, finish(y), SR, format="OGG", subtype="VORBIS")


def main():
    CAND.mkdir(parents=True, exist_ok=True)
    RAW.mkdir(parents=True, exist_ok=True)
    pitches = {1: 760, 2: 640, 3: 500}  # 1 bright (happy) ... 3 low and soft (tired/stiff)
    for style in "ABC":
        for n, p in pitches.items():
            save(chirp(p, style), CAND / f"bun_chirp_{style}_{n}.ogg")
        save(celebrate(style), CAND / f"celebrate_{style}.ogg")
        save(sparks(style), CAND / f"sparks_{style}.ogg")
    # style A goes into the app
    for n in pitches:
        save(chirp(pitches[n], "A"), RAW / f"bun_chirp_{n}.ogg")
    save(celebrate("A"), RAW / "celebrate.ogg")
    save(sparks("A"), RAW / "sparks.ogg")
    # placeholders for the Kenney slots
    save(note(880, 0.07, attack=0.003, decay=45, harm=(1.0, 0.1)), RAW / "tap_soft.ogg")
    save(note(660, 0.06, attack=0.003, decay=50, harm=(1.0, 0.0)), RAW / "toggle.ogg")
    save(mix([(0, note(523, 0.3, decay=9)), (0.12, note(784, 0.35, decay=8))], 0.5), RAW / "break_start.ogg")
    save(bells([659, 784, 1047], 0.09, dur=0.4, decay=7), RAW / "success.ogg")
    print("done:", sorted(p.name for p in RAW.glob("*.ogg")))


if __name__ == "__main__":
    main()
