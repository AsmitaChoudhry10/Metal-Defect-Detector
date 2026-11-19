"""Export the trained KSDD2 classifier with input normalization embedded."""
from pathlib import Path

import numpy as np
import torch
from PIL import Image
from torch import nn
from torchvision import models, transforms
import onnx


ROOT = Path(__file__).resolve().parents[1]
CHECKPOINT = ROOT / "models" / "kolektor_mobilenet_v3_small.pth"
DESTINATION = ROOT / "android-app" / "app" / "src" / "main" / "assets" / "kolektor_binary_mobilenet.onnx"
SAMPLE = ROOT / "data" / "raw" / "KolektorSDD2" / "test" / "20001.png"
HEIGHT, WIDTH = 192, 512


class InferenceGraph(nn.Module):
    def __init__(self, classifier):
        super().__init__()
        self.classifier = classifier
        self.register_buffer("mean", torch.tensor([0.485, 0.456, 0.406]).view(1, 3, 1, 1))
        self.register_buffer("std", torch.tensor([0.229, 0.224, 0.225]).view(1, 3, 1, 1))

    def forward(self, image_0_to_1):
        return self.classifier((image_0_to_1 - self.mean) / self.std)


def main():
    checkpoint = torch.load(CHECKPOINT, map_location="cpu", weights_only=True)
    model = models.mobilenet_v3_small(weights=None)
    model.classifier[3] = nn.Linear(model.classifier[3].in_features, 1)
    model.load_state_dict(checkpoint["state_dict"])
    graph = InferenceGraph(model.eval()).eval()

    image_tf = transforms.Compose([
        transforms.Grayscale(num_output_channels=3),
        transforms.Resize((HEIGHT, WIDTH), antialias=True),
        transforms.ToTensor(),
    ])
    with Image.open(SAMPLE) as image:
        sample = image_tf(image.convert("RGB")).unsqueeze(0)
    with torch.inference_mode():
        torch_result = graph(sample).numpy()

    DESTINATION.parent.mkdir(parents=True, exist_ok=True)
    torch.onnx.export(
        graph,
        sample,
        DESTINATION,
        input_names=["image_rgb_0_to_1"],
        output_names=["defect_logit"],
        opset_version=17,
        do_constant_folding=True,
        dynamo=False,
    )
    onnx.checker.check_model(onnx.load(DESTINATION))
    print(f"Exported {DESTINATION} ({DESTINATION.stat().st_size} bytes)")
    print(f"Reference PyTorch logit: {float(torch_result[0, 0]):.8f}")
    print("OpenCV DNN parity check is run separately with the installed OpenCV Python binding.")


if __name__ == "__main__":
    main()
