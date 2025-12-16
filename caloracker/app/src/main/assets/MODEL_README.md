# TensorFlow Lite Model Setup

## Required Files

This directory needs the following files for food detection:

1. **food_model.tflite** - The TensorFlow Lite model file
2. **food_labels.txt** - List of food class labels (already included)

## Downloading a Pre-trained Food Classification Model

You have several options to obtain a food classification model:

### Option 1: Food-101 Model from TensorFlow Hub

1. Visit TensorFlow Hub: https://tfhub.dev/
2. Search for "food" or "mobilenet"
3. Download a MobileNet or EfficientNet model trained on food images
4. Convert to TensorFlow Lite format if needed
5. Rename to `food_model.tflite` and place in this directory

### Option 2: Use a Pre-converted Model

Download a pre-trained food classification model:
- Search for "food-101 tflite" on GitHub
- Common sources:
  - https://github.com/Hvass-Labs/TensorFlow-Tutorials
  - https://www.kaggle.com/datasets (search for food-101)

### Option 3: Convert Your Own Model

If you have a TensorFlow or Keras model:

```python
import tensorflow as tf

# Load your model
model = tf.keras.models.load_model('your_food_model.h5')

# Convert to TensorFlow Lite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

# Save the model
with open('food_model.tflite', 'wb') as f:
    f.write(tflite_model)
```

### Option 4: Use MobileNetV2 Fine-tuned on Food-101

A lightweight option that works well on mobile devices:
- Model: MobileNetV2 (224x224 input)
- Dataset: Food-101 (101 food categories)
- Size: ~4-14 MB (depending on optimization)

## Model Requirements

Your TensorFlow Lite model should:
- Accept input size: 224x224x3 or 299x299x3 RGB images
- Output: Float array with confidence scores for each class
- Input format: Normalized [0,1] or [-1,1] range
- Quantized models (uint8) are supported for smaller size

## Expected Input/Output

**Input:**
- Shape: [1, 224, 224, 3] or [1, 299, 299, 3]
- Type: Float32 or Uint8
- Range: [0, 1] for float, [0, 255] for uint8

**Output:**
- Shape: [1, num_classes]
- Type: Float32
- Values: Confidence scores (0-1) for each class

## Testing Your Model

Once you place `food_model.tflite` in this directory:
1. Build and run the app
2. Take a photo of food
3. The app will display top 5 predictions with confidence scores

## Notes

- The `food_labels.txt` file provided contains Food-101 class names
- If you use a different model, update `food_labels.txt` to match your model's classes
- Ensure the order of labels matches your model's output order
- Typical model sizes: 3-20 MB
