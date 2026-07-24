from pathlib import Path

import numpy as np
from PIL import Image
import torch
import torch.nn as nn


class CRNN(nn.Module):
    def __init__(self, num_classes, hidden_size=256):
        super().__init__()

        self.cnn = nn.Sequential(
            nn.Conv2d(1, 64, 3, 1, 1),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(2, 2),

            nn.Conv2d(64, 128, 3, 1, 1),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(2, 2),

            nn.Conv2d(128, 256, 3, 1, 1),
            nn.BatchNorm2d(256),
            nn.ReLU(inplace=True),

            nn.Conv2d(256, 256, 3, 1, 1),
            nn.ReLU(inplace=True),
            nn.MaxPool2d((2, 1), (2, 1)),

            nn.Conv2d(256, 512, 3, 1, 1),
            nn.BatchNorm2d(512),
            nn.ReLU(inplace=True),

            nn.Conv2d(512, 512, 3, 1, 1),
            nn.BatchNorm2d(512),
            nn.ReLU(inplace=True),
            nn.MaxPool2d((2, 1), (2, 1)),

            nn.Conv2d(512, 512, 2, 1, 0),
            nn.ReLU(inplace=True),
        )

        self.rnn = nn.LSTM(
            input_size=512,
            hidden_size=hidden_size,
            num_layers=2,
            bidirectional=True,
            batch_first=False,
        )

        self.classifier = nn.Linear(hidden_size * 2, num_classes)

    def forward(self, images):
        features = self.cnn(images)
        features = features.squeeze(2)
        features = features.permute(2, 0, 1)
        recurrent, _ = self.rnn(features)
        return self.classifier(recurrent)


class CRNNRecognizer:
    def __init__(self, model_path):
        self.model_path = Path(model_path)
        if not self.model_path.exists():
            raise FileNotFoundError(self.model_path)

        self.device = torch.device(
            "cuda" if torch.cuda.is_available() else "cpu"
        )
        checkpoint = torch.load(
            self.model_path,
            map_location=self.device,
        )

        self.charset = checkpoint["charset"]
        self.width = int(checkpoint.get("width", 160))
        self.height = int(checkpoint.get("height", 32))

        self.idx_to_char = {
            index + 1: char
            for index, char in enumerate(self.charset)
        }

        self.model = CRNN(
            num_classes=len(self.charset) + 1
        ).to(self.device)
        self.model.load_state_dict(checkpoint["model"])
        self.model.eval()

    def _to_tensor(self, image):
        if isinstance(image, (str, Path)):
            image = Image.open(image).convert("L")
        else:
            image = Image.fromarray(image).convert("L")

        image = image.resize(
            (self.width, self.height),
            Image.Resampling.BICUBIC,
        )
        array = np.asarray(image, dtype=np.float32) / 255.0
        tensor = torch.from_numpy(array).unsqueeze(0)
        tensor = (tensor - 0.5) / 0.5
        return tensor.unsqueeze(0).to(self.device)

    def predict(self, image):
        tensor = self._to_tensor(image)

        with torch.no_grad():
            logits = self.model(tensor)
            probabilities = logits.softmax(2)
            max_probs, best_ids = probabilities.max(2)

        best_ids = best_ids[:, 0].detach().cpu().tolist()
        max_probs = max_probs[:, 0].detach().cpu().tolist()

        previous = 0
        chars = []
        selected_probs = []

        for current, probability in zip(best_ids, max_probs):
            current = int(current)
            if current != 0 and current != previous:
                chars.append(self.idx_to_char.get(current, ""))
                selected_probs.append(float(probability))
            previous = current

        text = "".join(chars)
        confidence = (
            sum(selected_probs) / len(selected_probs)
            if selected_probs else 0.0
        )

        return {
            "raw": text,
            "text": text,
            "confidence": float(confidence),
        }
