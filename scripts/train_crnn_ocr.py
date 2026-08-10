import argparse
import csv
import json
import random
import re
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageEnhance, ImageFilter

import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, Dataset


HANGUL_PLATE_FILTER = re.compile("[^0-9A-Za-z\uAC00-\uD7A3]")
KOREAN_PLATE_EXPANDED_CHARSET = (
    "0123456789"
    "가강거경계고관광구금기김나남너노누다대더도동두등"
    "라러로루마머명모무문미바배버보부북사산서소수아악안양"
    "어연영오용우울원육인자작저전조주중지차천초추충카타파평포하허호홀"
    "서울부산대구인천광주대전울산세종경기강원충북충남전북전남경북경남제주"
)


def resolve(path: str) -> Path:
    candidate = Path(path)
    if candidate.is_absolute():
        return candidate
    return Path.cwd() / candidate


def normalize_plate(value: str) -> str:
    return HANGUL_PLATE_FILTER.sub("", value or "").upper()


def load_rows(labels_csv: Path):
    with labels_csv.open("r", encoding="utf-8-sig", newline="") as file:
        rows = list(csv.DictReader(file))

    clean_rows = []
    for row in rows:
        image = row.get("image") or row.get("path") or row.get("filename")
        label = normalize_plate(row.get("label") or row.get("answer") or "")
        split = (row.get("split") or "").lower()
        if not image or not label:
            continue
        clean_rows.append({"image": image, "label": label, "split": split})
    return clean_rows


def build_charset(rows):
    chars = sorted({char for row in rows for char in row["label"]})
    return chars


def unique_chars(value: str):
    chars = []
    seen = set()
    for char in value:
        if char in seen:
            continue
        seen.add(char)
        chars.append(char)
    return chars


def build_training_charset(rows, fixed_charset: str = "", charset_preset: str = ""):
    if fixed_charset:
        return unique_chars(fixed_charset)

    charset_source = ""
    if charset_preset == "korean_plate_expanded":
        charset_source += KOREAN_PLATE_EXPANDED_CHARSET
    charset_source += "".join(build_charset(rows))

    if charset_source:
        return unique_chars(charset_source)
    return build_charset(rows)


class PlateDataset(Dataset):
    def __init__(
        self,
        rows,
        root: Path,
        char_to_idx,
        width: int,
        height: int,
        augment: bool = False,
        allow_unknown_target: bool = False,
    ):
        self.rows = rows
        self.root = root
        self.char_to_idx = char_to_idx
        self.width = width
        self.height = height
        self.augment = augment
        self.allow_unknown_target = allow_unknown_target

    def __len__(self):
        return len(self.rows)

    def __getitem__(self, index):
        row = self.rows[index]
        image_path = Path(row["image"])
        if not image_path.is_absolute():
            image_path = self.root / image_path
        image = Image.open(image_path).convert("L")
        if self.augment:
            image = augment_image(image)
        image = image.resize((self.width, self.height), Image.Resampling.BICUBIC)
        tensor = torch.tensor(list(image.getdata()), dtype=torch.float32)
        tensor = tensor.view(1, self.height, self.width) / 255.0
        if self.augment and random.random() < 0.20:
            tensor = (tensor + torch.randn_like(tensor) * 0.025).clamp(0.0, 1.0)
        tensor = (tensor - 0.5) / 0.5
        if self.allow_unknown_target:
            target = torch.tensor([self.char_to_idx[c] for c in row["label"] if c in self.char_to_idx], dtype=torch.long)
        else:
            target = torch.tensor([self.char_to_idx[c] for c in row["label"]], dtype=torch.long)
        return {
            "image": tensor,
            "target": target,
            "label": row["label"],
            "path": str(image_path),
        }


def augment_image(image: Image.Image) -> Image.Image:
    if random.random() < 0.30:
        image = ImageEnhance.Brightness(image).enhance(random.uniform(0.75, 1.10))
    if random.random() < 0.30:
        image = ImageEnhance.Contrast(image).enhance(random.uniform(0.80, 1.20))
    if random.random() < 0.15:
        image = image.filter(ImageFilter.GaussianBlur(radius=random.uniform(0.2, 0.7)))
    return image


def collate_batch(batch):
    images = torch.stack([item["image"] for item in batch])
    targets = torch.cat([item["target"] for item in batch])
    target_lengths = torch.tensor([len(item["target"]) for item in batch], dtype=torch.long)
    labels = [item["label"] for item in batch]
    paths = [item["path"] for item in batch]
    return images, targets, target_lengths, labels, paths


class CRNN(nn.Module):
    def __init__(self, num_classes: int, hidden_size: int = 256):
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
        logits = self.classifier(recurrent)
        return logits


def load_init_checkpoint(model: CRNN, init_path: Path, new_charset, device):
    checkpoint = torch.load(init_path, map_location=device)
    old_state = checkpoint["model"]
    new_state = model.state_dict()
    copied = []
    skipped = []

    for key, value in old_state.items():
        if key.startswith("classifier."):
            continue
        if key in new_state and new_state[key].shape == value.shape:
            new_state[key] = value
            copied.append(key)
        else:
            skipped.append(key)

    old_charset = checkpoint.get("charset", [])
    old_char_to_index = {char: index + 1 for index, char in enumerate(old_charset)}
    new_char_to_index = {char: index + 1 for index, char in enumerate(new_charset)}

    if "classifier.weight" in old_state and "classifier.bias" in old_state:
        old_weight = old_state["classifier.weight"]
        old_bias = old_state["classifier.bias"]
        new_weight = new_state["classifier.weight"]
        new_bias = new_state["classifier.bias"]

        new_weight[0] = old_weight[0]
        new_bias[0] = old_bias[0]
        copied.append("classifier.blank")

        copied_chars = 0
        for char, old_index in old_char_to_index.items():
            new_index = new_char_to_index.get(char)
            if new_index is None:
                continue
            new_weight[new_index] = old_weight[old_index]
            new_bias[new_index] = old_bias[old_index]
            copied_chars += 1
        copied.append(f"classifier.chars:{copied_chars}")

    model.load_state_dict(new_state)
    return {
        "path": str(init_path),
        "copied": copied,
        "skipped": skipped,
        "old_charset_size": len(old_charset),
        "new_charset_size": len(new_charset),
    }


def greedy_decode(logits, idx_to_char):
    best = logits.softmax(2).argmax(2).detach().cpu().tolist()
    batch_size = len(best[0])
    predictions = []
    for batch_index in range(batch_size):
        previous = 0
        chars = []
        for timestep in range(len(best)):
            current = best[timestep][batch_index]
            if current != 0 and current != previous:
                chars.append(idx_to_char[str(current)])
            previous = current
        predictions.append("".join(chars))
    return predictions


def edit_distance(left: str, right: str) -> int:
    previous = list(range(len(right) + 1))
    for i, left_char in enumerate(left, start=1):
        current = [i]
        for j, right_char in enumerate(right, start=1):
            insert_cost = current[j - 1] + 1
            delete_cost = previous[j] + 1
            replace_cost = previous[j - 1] + (left_char != right_char)
            current.append(min(insert_cost, delete_cost, replace_cost))
        previous = current
    return previous[-1]


@torch.no_grad()
def evaluate(model, loader, device, idx_to_char, report_path=None):
    model.eval()
    total = 0
    correct = 0
    total_chars = 0
    total_edit_distance = 0
    rows = []
    for images, _, _, labels, paths in loader:
        images = images.to(device)
        logits = model(images)
        predictions = greedy_decode(logits, idx_to_char)
        for path, label, prediction in zip(paths, labels, predictions):
            normalized_label = normalize_plate(label)
            normalized_prediction = normalize_plate(prediction)
            is_correct = normalized_label == normalized_prediction
            distance = edit_distance(normalized_label, normalized_prediction)
            total += 1
            correct += int(is_correct)
            total_chars += len(normalized_label)
            total_edit_distance += distance
            rows.append(
                {
                    "image": path,
                    "label": label,
                    "prediction": prediction,
                    "normalized_label": normalized_label,
                    "normalized_prediction": normalized_prediction,
                    "edit_distance": distance,
                    "is_correct": str(is_correct),
                }
            )

    accuracy = (correct / total * 100.0) if total else 0.0
    char_accuracy = (
        max(0.0, (1.0 - (total_edit_distance / total_chars)) * 100.0)
        if total_chars
        else 0.0
    )
    avg_edit_distance = (total_edit_distance / total) if total else 0.0
    if report_path:
        report_path.parent.mkdir(parents=True, exist_ok=True)
        with report_path.open("w", encoding="utf-8-sig", newline="") as file:
            writer = csv.DictWriter(
                file,
                fieldnames=[
                    "image",
                    "label",
                    "prediction",
                    "normalized_label",
                    "normalized_prediction",
                    "edit_distance",
                    "is_correct",
                ],
            )
            writer.writeheader()
            writer.writerows(rows)
    return accuracy, correct, total, char_accuracy, avg_edit_distance


@dataclass
class TrainState:
    best_accuracy: float = 0.0
    best_epoch: int = 0


def main():
    parser = argparse.ArgumentParser(description="Train a small CRNN OCR model for Korean license plates.")
    parser.add_argument("--labels-csv", default="ocr_test_10000/labels.csv")
    parser.add_argument("--output", default="runs/ocr/crnn_plate")
    parser.add_argument("--width", type=int, default=160)
    parser.add_argument("--height", type=int, default=32)
    parser.add_argument("--epochs", type=int, default=40)
    parser.add_argument("--batch-size", type=int, default=64)
    parser.add_argument("--lr", type=float, default=0.001)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--device", default="auto", choices=["auto", "cpu", "cuda"])
    parser.add_argument("--charset", default="", help="Optional fixed OCR charset. If empty, labels.csv is used.")
    parser.add_argument(
        "--charset-preset",
        default="",
        choices=["", "korean_plate_expanded"],
        help="Optional charset preset. korean_plate_expanded includes common Korean plate and region-name characters.",
    )
    parser.add_argument("--init-from", default="", help="Optional checkpoint to initialize matching weights from.")
    parser.add_argument("--augment", action="store_true", help="Use light OCR image augmentation for training only.")
    args = parser.parse_args()

    random.seed(args.seed)
    torch.manual_seed(args.seed)

    labels_csv = resolve(args.labels_csv)
    root = Path.cwd()
    output = resolve(args.output)
    output.mkdir(parents=True, exist_ok=True)

    rows = load_rows(labels_csv)
    if not rows:
        raise SystemExit(f"No valid rows found in {labels_csv}")

    train_rows = [row for row in rows if row["split"] == "train"]
    val_rows = [row for row in rows if row["split"] == "val"]
    if not train_rows or not val_rows:
        shuffled = rows[:]
        random.shuffle(shuffled)
        split_index = max(1, int(len(shuffled) * 0.9))
        train_rows = shuffled[:split_index]
        val_rows = shuffled[split_index:]

    charset = build_training_charset(rows, fixed_charset=args.charset, charset_preset=args.charset_preset)
    char_to_idx = {char: index + 1 for index, char in enumerate(charset)}
    idx_to_char = {str(index + 1): char for index, char in enumerate(charset)}
    idx_to_char["0"] = ""

    with (output / "charset.json").open("w", encoding="utf-8") as file:
        json.dump(
            {
                "blank": 0,
                "chars": charset,
                "char_to_idx": char_to_idx,
                "idx_to_char": idx_to_char,
                "width": args.width,
                "height": args.height,
            },
            file,
            ensure_ascii=False,
            indent=2,
        )

    device = args.device
    if device == "auto":
        device = "cuda" if torch.cuda.is_available() else "cpu"
    device = torch.device(device)

    train_dataset = PlateDataset(train_rows, root, char_to_idx, args.width, args.height, augment=args.augment)
    val_dataset = PlateDataset(val_rows, root, char_to_idx, args.width, args.height)
    train_loader = DataLoader(
        train_dataset,
        batch_size=args.batch_size,
        shuffle=True,
        num_workers=0,
        collate_fn=collate_batch,
    )
    val_loader = DataLoader(
        val_dataset,
        batch_size=args.batch_size,
        shuffle=False,
        num_workers=0,
        collate_fn=collate_batch,
    )

    model = CRNN(num_classes=len(charset) + 1).to(device)
    init_info = None
    if args.init_from:
        init_info = load_init_checkpoint(model, resolve(args.init_from), charset, device)
    criterion = nn.CTCLoss(blank=0, zero_infinity=True)
    optimizer = optim.Adam(model.parameters(), lr=args.lr)
    state = TrainState()

    print(f"Device: {device}")
    print(f"Train: {len(train_dataset)} Val: {len(val_dataset)} Augment: {args.augment} Charset: {''.join(charset)}")
    if init_info:
        print(
            "Init-from: "
            f"{init_info['path']} "
            f"old_charset={init_info['old_charset_size']} "
            f"new_charset={init_info['new_charset_size']}"
        )

    log_path = output / "train_log.csv"
    with log_path.open("w", encoding="utf-8-sig", newline="") as log_file:
        writer = csv.DictWriter(
            log_file,
            fieldnames=[
                "epoch",
                "train_loss",
                "val_accuracy",
                "val_char_accuracy",
                "val_avg_edit_distance",
                "val_correct",
                "val_total",
            ],
        )
        writer.writeheader()

        for epoch in range(1, args.epochs + 1):
            model.train()
            losses = []
            for images, targets, target_lengths, _, _ in train_loader:
                images = images.to(device)
                targets = targets.to(device)
                target_lengths = target_lengths.to(device)

                logits = model(images)
                log_probs = logits.log_softmax(2)
                input_lengths = torch.full(
                    size=(images.size(0),),
                    fill_value=logits.size(0),
                    dtype=torch.long,
                    device=device,
                )

                loss = criterion(log_probs, targets, input_lengths, target_lengths)
                optimizer.zero_grad()
                loss.backward()
                nn.utils.clip_grad_norm_(model.parameters(), 5.0)
                optimizer.step()
                losses.append(float(loss.detach().cpu()))

            train_loss = sum(losses) / len(losses)
            val_accuracy, val_correct, val_total, val_char_accuracy, val_avg_edit_distance = evaluate(
                model, val_loader, device, idx_to_char
            )
            writer.writerow(
                {
                    "epoch": epoch,
                    "train_loss": f"{train_loss:.6f}",
                    "val_accuracy": f"{val_accuracy:.2f}",
                    "val_char_accuracy": f"{val_char_accuracy:.2f}",
                    "val_avg_edit_distance": f"{val_avg_edit_distance:.3f}",
                    "val_correct": val_correct,
                    "val_total": val_total,
                }
            )
            log_file.flush()

            print(
                f"Epoch {epoch}/{args.epochs} "
                f"loss={train_loss:.4f} "
                f"val_acc={val_accuracy:.2f}% ({val_correct}/{val_total}) "
                f"char_acc={val_char_accuracy:.2f}% "
                f"edit={val_avg_edit_distance:.2f}"
            )

            if val_accuracy >= state.best_accuracy:
                state.best_accuracy = val_accuracy
                state.best_epoch = epoch
                torch.save(
                    {
                        "model": model.state_dict(),
                        "charset": charset,
                        "width": args.width,
                        "height": args.height,
                        "epoch": epoch,
                        "val_accuracy": val_accuracy,
                        "init_from": init_info,
                    },
                    output / "best.pt",
                )

    checkpoint = torch.load(output / "best.pt", map_location=device)
    model.load_state_dict(checkpoint["model"])
    report_path = output / "evaluation.csv"
    val_accuracy, val_correct, val_total, val_char_accuracy, val_avg_edit_distance = evaluate(
        model, val_loader, device, idx_to_char, report_path=report_path
    )
    summary = (
        f"Experiment conditions\n"
        f"Labels CSV: {labels_csv}\n"
        f"Output: {output}\n"
        f"Requested epochs: {args.epochs}\n"
        f"Best epoch: {checkpoint.get('epoch', state.best_epoch)}\n"
        f"Batch size: {args.batch_size}\n"
        f"Learning rate: {args.lr}\n"
        f"Seed: {args.seed}\n"
        f"Device: {device}\n"
        f"Width: {args.width}\n"
        f"Height: {args.height}\n"
        f"Train rows: {len(train_rows)}\n"
        f"Validation rows: {len(val_rows)}\n"
        f"Augmentation: {args.augment}\n"
        f"Charset preset: {args.charset_preset or '(none)'}\n"
        f"Fixed charset argument: {'yes' if args.charset else 'no'}\n"
        f"Charset size: {len(charset)}\n"
        f"Init from: {resolve(args.init_from) if args.init_from else '(none)'}\n"
        f"\n"
        f"Best validation result\n"
        f"Best epoch: {state.best_epoch}\n"
        f"Validation total: {val_total}\n"
        f"Validation correct: {val_correct}\n"
        f"Validation accuracy: {val_accuracy:.2f}%\n"
        f"Validation char accuracy: {val_char_accuracy:.2f}%\n"
        f"Validation avg edit distance: {val_avg_edit_distance:.3f}\n"
        f"Model: {output / 'best.pt'}\n"
        f"Report: {report_path}\n"
    )
    (output / "summary.txt").write_text(summary, encoding="utf-8")
    print(summary)


if __name__ == "__main__":
    main()
