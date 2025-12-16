# Caloracker - Free Food Tracking with AI

Caloracker is a completely **free** Android app for tracking daily calorie and macronutrient intake using on-device AI food detection and public nutrition data.

## Features

🎯 **100% Free - No API Keys Required**
- Food detection runs locally on your device using TensorFlow Lite
- Nutrition data from free USDA FoodData Central API
- No subscriptions, no rate limits, no hidden costs

🔒 **Privacy-First**
- Images never leave your device
- Food detection happens entirely on-device
- No user data sent to external servers (except nutrition lookups)

📸 **Smart Food Detection**
- Take a photo of your food
- Get instant AI predictions (top 5 suggestions)
- Confirm and get accurate nutrition data

📊 **Comprehensive Tracking**
- Daily calorie and macro tracking (protein, carbs, fat)
- Multi-day history view
- Visual progress indicators
- Goal setting and monitoring

## Tech Stack

- **Language**: Kotlin
- **Architecture**: MVVM with Repository pattern
- **Database**: Room (SQLite) for offline storage
- **Machine Learning**: TensorFlow Lite (on-device food detection)
- **Nutrition API**: USDA FoodData Central (free, no auth)
- **UI**: Material Design 3 with ViewBinding
- **Camera**: CameraX for image capture
- **Networking**: Retrofit + OkHttp (USDA API only)
- **Async**: Kotlin Coroutines and Flow

## Setup Instructions

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17 or later
- Android SDK 34
- Minimum Android version: 7.0 (API 24)

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/caloracker.git
cd caloracker
```

### 2. Download TensorFlow Lite Model

The app requires a TensorFlow Lite food classification model:

1. Download a food classification model (e.g., Food-101 MobileNetV2)
2. Place the model file at: `app/src/main/assets/food_model.tflite`
3. The labels file is already included at: `app/src/main/assets/food_labels.txt`

**Model Sources:**
- TensorFlow Hub: https://tfhub.dev/ (search for "food" or "mobilenet")
- Kaggle: Search for "food-101 tflite"
- GitHub: Search for "food classification tflite"

**Model Requirements:**
- Input: 224x224x3 or 299x299x3 RGB images
- Output: Float array with confidence scores
- Format: TensorFlow Lite (.tflite)
- Recommended: MobileNetV2 or EfficientNet for mobile performance

See `app/src/main/assets/MODEL_README.md` for detailed instructions.

### 3. Build and Run

```bash
# Open in Android Studio
# OR build from command line:
./gradlew assembleDebug

# Install on device/emulator
./gradlew installDebug
```

**That's it!** No API keys, no configuration needed. The app is ready to use.

## Usage

### Taking a Food Photo

1. Open the app and tap the camera button
2. Take a clear photo of your food
3. The TensorFlow Lite model analyzes the image on-device (instant results)
4. Review the top 5 AI predictions with confidence scores

### Confirming Food Entry

1. Select the correct food from AI suggestions (or search manually)
2. The app queries USDA API for accurate nutrition data
3. Adjust portion size if needed
4. Confirm to log the entry

### Viewing History

- Navigate to the History screen
- View daily totals and individual entries
- Edit or delete past entries
- Track progress toward your goals

## How It Works

### 1. On-Device Food Detection

When you take a photo:
1. Image is preprocessed (resized, normalized)
2. TensorFlow Lite model runs inference locally
3. Returns top 5 predictions with confidence scores
4. **No internet required** for this step

### 2. Nutrition Lookup

When you confirm a food:
1. App queries USDA FoodData Central API
2. Returns calories, protein, carbs, fat per 100g
3. Scales to your selected portion size
4. **Fallback to manual entry** if exact match not found

### 3. Local Storage

All food logs stored locally in Room database:
- Works offline after initial setup
- Fast queries and updates
- Exportable data

## API Information

### USDA FoodData Central API

- **URL**: https://api.nal.usda.gov/fdc/v1/
- **Authentication**: None (completely free and open)
- **Rate Limits**: Generous limits for personal use
- **Documentation**: https://fdc.nal.usda.gov/api-guide.html

The USDA API is a public service providing:
- 350,000+ food items
- Accurate nutrition data
- Regular updates
- No registration required

## Project Structure

```
com.caloracker/
├── data/
│   ├── local/          # Room database
│   ├── remote/         # USDA API service
│   ├── ml/             # TensorFlow Lite service
│   └── repository/     # Data repositories
├── domain/
│   └── model/          # Domain models
├── ui/
│   ├── today/          # Today's tracking screen
│   ├── history/        # History screen
│   ├── confirmation/   # Food confirmation screen
│   └── adapters/       # RecyclerView adapters
└── util/               # Utilities and helpers
```

## Development

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

### Code Style

This project follows [Google's Kotlin Style Guide](https://developer.android.com/kotlin/style-guide).

See [CLAUDE.md](CLAUDE.md) for detailed development guidelines.

### Building Release APK

```bash
./gradlew assembleRelease
```

## Troubleshooting

### Model Not Found Error

**Problem**: App crashes on startup with "food_model.tflite not found"

**Solution**: Download and place a TensorFlow Lite model at `app/src/main/assets/food_model.tflite`

See `app/src/main/assets/MODEL_README.md` for download instructions.

### USDA API Returns No Results

**Problem**: Food search returns empty results

**Solutions**:
- Check internet connection (USDA API requires network)
- Try different search terms (e.g., "apple" instead of "gala apple")
- Use manual entry as fallback
- Check USDA API status: https://fdc.nal.usda.gov/

### Camera Permission Denied

**Problem**: Camera doesn't open

**Solution**: Grant camera permission in Android Settings > Apps > Caloracker > Permissions

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Follow the code style guidelines
4. Write tests for new features
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

## Privacy Policy

Caloracker respects your privacy:

- ✅ Food images processed locally on your device
- ✅ No user tracking or analytics
- ✅ No personal data collection
- ✅ Food logs stored only on your device
- ✅ Only nutrition data queries sent to USDA (no personal info)
- ✅ Open source - verify yourself!

## License

This project is licensed under the MIT License.

## Acknowledgments

- **TensorFlow Lite** for on-device ML capabilities
- **USDA FoodData Central** for free nutrition data
- **Food-101** dataset creators for food classification training data
- **Material Design** team for beautiful UI components

---

**Made with ❤️ for the health-conscious community**

*Remember: This app is completely free forever. No subscriptions, no ads, no hidden costs.*
