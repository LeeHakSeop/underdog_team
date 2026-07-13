from __future__ import annotations

import cv2
import numpy as np


AVAILABLE_PREPROCESS_MODES = ["none", "gray", "clahe", "sharpen", "denoise", "threshold"]


def _to_gray(image: np.ndarray) -> np.ndarray:
    if len(image.shape) == 2:
        return image
    return cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)


def _to_bgr(gray: np.ndarray) -> np.ndarray:
    return cv2.cvtColor(gray, cv2.COLOR_GRAY2BGR)


def split_preprocess_mode(mode: str) -> tuple[str, str]:
    parts = (mode or "none").lower().split(":", 1)
    name = parts[0].strip()
    value = parts[1].strip() if len(parts) == 2 else ""
    return name, value


def preprocess_mode_name(mode: str) -> str:
    name, _ = split_preprocess_mode(mode)
    return name


def safe_preprocess_label(mode: str) -> str:
    return (mode or "none").lower().replace(":", "_").replace(".", "p")


def preprocess_plate_image(image: np.ndarray, mode: str) -> np.ndarray:
    name, value = split_preprocess_mode(mode)
    if name not in AVAILABLE_PREPROCESS_MODES:
        raise ValueError(f"Unknown preprocess mode: {mode}")

    if name == "none":
        return image

    gray = _to_gray(image)

    if name == "gray":
        return _to_bgr(gray)

    if name == "clahe":
        clip_limit = float(value) if value else 2.0
        clahe = cv2.createCLAHE(clipLimit=clip_limit, tileGridSize=(8, 8))
        return _to_bgr(clahe.apply(gray))

    if name == "sharpen":
        amount = float(value) if value else 0.5
        blurred = cv2.GaussianBlur(gray, (0, 0), 1.0)
        sharpened = cv2.addWeighted(gray, 1.0 + amount, blurred, -amount, 0)
        return _to_bgr(sharpened)

    if name == "denoise":
        strength = float(value) if value else 7.0
        denoised = cv2.fastNlMeansDenoising(gray, None, h=strength, templateWindowSize=7, searchWindowSize=21)
        return _to_bgr(denoised)

    if name == "threshold":
        c_value = float(value) if value else 7.0
        thresholded = cv2.adaptiveThreshold(
            gray,
            255,
            cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
            cv2.THRESH_BINARY,
            31,
            c_value,
        )
        return _to_bgr(thresholded)

    return image
