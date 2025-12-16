# Caloracker - Android Project Guidelines

## Overview
Caloracker is an Android application for tracking daily calorie and macronutrient intake through camera-based food detection.

## Tech Stack

### Language
- **Kotlin** (modern Android standard)
- Target SDK: 34 (Android 14)
- Minimum SDK: 24 (Android 7.0)

### Architecture
- **Pattern**: MVVM (Model-View-ViewModel) with Repository pattern
- **Navigation**: Jetpack Navigation Component
- **Dependency Injection**: Manual DI (can be upgraded to Hilt/Dagger later)

### Core Libraries

#### Database
- **Room ORM** for local SQLite storage
- Type converters for Date and complex types
- Database versioning and migrations

#### Networking
- **Retrofit** for REST API calls
- **OkHttp** for HTTP client and logging interceptor
- **Gson** or **Moshi** for JSON serialization

#### Machine Learning (On-Device)
- **TensorFlow Lite** for local food detection (no API calls required)
- **Food-101 Model** or similar pre-trained food classification model
- Runs entirely on-device for instant predictions
- Returns top 5 predictions with confidence scores
- No internet required for food detection
- Privacy-first approach - images never leave the device

#### API Integration
- **USDA FoodData Central API** (free, no authentication required)
- Used only for nutrition data lookup after food confirmation
- Provides accurate calorie and macronutrient information
- No API key needed - completely free and open
- Fallback to manual entry if exact match not found

#### UI
- **Material Design 3** components
- **ViewBinding** for type-safe view access
- **RecyclerView** with DiffUtil for efficient lists
- **Coil** for image loading and caching

#### Image Processing
- **CameraX** for camera functionality
- **Bitmap processing** for image preprocessing before TensorFlow Lite inference
- Image resizing and normalization for model input (typically 224x224 or 299x299)
- Efficient memory management for on-device processing

#### Lifecycle
- **Lifecycle-aware components** (ViewModel, LiveData)
- **Kotlin Coroutines** for asynchronous operations
- **Flow** for reactive data streams

#### Testing
- **JUnit 4/5** for unit tests
- **Espresso** for UI tests
- **MockK** for mocking in Kotlin
- **Room Testing** utilities

## Project Structure

Organize code by feature using the following package structure:

```
com.caloracker/
├── ui/                          # Presentation layer
│   ├── MainActivity.kt
│   ├── today/                   # Today's tracking screen
│   │   ├── TodayFragment.kt
│   │   └── TodayViewModel.kt
│   ├── history/                 # Historical data screen
│   │   ├── HistoryFragment.kt
│   │   └── HistoryViewModel.kt
│   ├── confirmation/            # Food confirmation screen
│   │   ├── FoodConfirmationFragment.kt
│   │   └── FoodConfirmationViewModel.kt
│   └── adapters/                # RecyclerView adapters
│       ├── FoodLogAdapter.kt
│       └── FoodSuggestionAdapter.kt
├── data/                        # Data layer
│   ├── local/                   # Local database
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   │   └── FoodLogDao.kt
│   │   └── entity/
│   │       └── FoodLog.kt
│   ├── remote/                  # API services
│   │   ├── UsdaApi.kt
│   │   └── dto/                 # Data transfer objects
│   │       ├── UsdaFoodRequest.kt
│   │       └── UsdaFoodResponse.kt
│   ├── ml/                      # Machine learning services
│   │   └── FoodDetectionService.kt
│   └── repository/              # Repository implementations
│       ├── FoodRepository.kt
│       └── NutritionRepository.kt
├── domain/                      # Business logic layer
│   ├── model/                   # Domain models
│   │   ├── Food.kt
│   │   └── NutritionInfo.kt
│   └── usecase/                 # Use cases (if needed)
└── util/                        # Utilities and helpers
    ├── Constants.kt
    ├── DateUtils.kt
    ├── NetworkUtils.kt
    ├── ImageUtils.kt            # Bitmap processing and normalization
    ├── ModelUtils.kt            # TensorFlow Lite model helper functions
    └── Extensions.kt
```

## Key Features

### 1. On-Device Food Detection via TensorFlow Lite
- Use CameraX to capture food images
- Process image locally with TensorFlow Lite model
- Get instant predictions without internet connection
- Display top 5 food predictions with confidence scores
- Privacy-first: images never leave the device
- No API costs or rate limits

### 2. Food Confirmation with ML Predictions
- Show TensorFlow Lite's top food prediction
- Display 4-5 alternative suggestions with confidence scores
- Allow user to select correct food or search manually
- Specify portion size
- Fetch nutrition data from USDA API for selected food
- Confirm and log the entry

### 3. Calorie & Macro Tracking
- Track: calories, protein, carbohydrates, fat
- Daily goal setting
- Real-time progress visualization
- Visual progress bars or charts

### 4. Multi-Day History
- View logs by date
- Summary statistics per day
- Edit or delete past entries
- Search and filter capabilities

### 5. Data Persistence
- Local Room database for offline access
- Cache USDA nutrition data for faster lookups
- Store TensorFlow Lite predictions for user learning
- Offline-first architecture with on-device ML

## Code Style

### Google's Kotlin Style Guide
- Follow [Google's Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
- Use 4 spaces for indentation
- Maximum line length: 100 characters
- Use camelCase for variables and functions
- Use PascalCase for classes

### Best Practices
- Single Responsibility Principle for classes
- Dependency injection over hardcoded dependencies
- Use sealed classes for UI states
- Use data classes for models
- Prefer immutability (val over var)
- Use extension functions to enhance readability
- Coroutines for background operations (not callbacks)
- Proper error handling with sealed Result classes

### Naming Conventions
- **Activities**: `*Activity.kt` (e.g., MainActivity.kt)
- **Fragments**: `*Fragment.kt` (e.g., TodayFragment.kt)
- **ViewModels**: `*ViewModel.kt` (e.g., TodayViewModel.kt)
- **Adapters**: `*Adapter.kt` (e.g., FoodLogAdapter.kt)
- **Layouts**: `activity_*.xml`, `fragment_*.xml`, `item_*.xml`
- **Resources**: Use snake_case for all resource files

## Database Schema

### FoodLog Table
```kotlin
@Entity(tableName = "food_logs")
data class FoodLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,              // Unix timestamp
    val foodName: String,
    val portion: String,         // e.g., "100g", "1 cup"
    val calories: Double,
    val protein: Double,         // grams
    val carbs: Double,          // grams
    val fat: Double,            // grams
    val imageUri: String? = null // Optional food image
)
```

## API Configuration

### USDA FoodData Central API
- **Base URL**: `https://api.nal.usda.gov/fdc/v1/`
- **Authentication**: None required (completely free and open)
- **No API key needed** for basic searches
- **Rate limits**: Reasonable limits for free tier (sufficient for personal use)
- **Documentation**: https://fdc.nal.usda.gov/api-guide.html

#### Endpoints Used
- **Search Foods**: `GET /foods/search?query={food_name}`
- **Food Details**: `GET /food/{fdc_id}`

### Example API Request
```kotlin
// Search for a food
GET https://api.nal.usda.gov/fdc/v1/foods/search?query=apple&dataType=Foundation,SR%20Legacy

// Response includes:
{
  "foods": [
    {
      "fdcId": 123456,
      "description": "Apple, raw",
      "foodNutrients": [
        {
          "nutrientName": "Energy",
          "value": 52,
          "unitName": "kcal"
        },
        {
          "nutrientName": "Protein",
          "value": 0.26,
          "unitName": "g"
        },
        // ... more nutrients
      ]
    }
  ]
}
```

### TensorFlow Lite Model Configuration
- **Model**: Food-101 or MobileNetV2 trained on food images
- **Location**: `app/src/main/assets/food_model.tflite`
- **Labels**: `app/src/main/assets/food_labels.txt`
- **Input**: 224x224 RGB image (normalized to [0,1])
- **Output**: Array of confidence scores for each food class
- **Preprocessing**: Resize, normalize, convert to float array

### Response Processing
- TensorFlow Lite returns confidence scores for all food classes
- Sort and select top 5 predictions
- Display predictions with confidence percentages
- On user confirmation, query USDA API for selected food
- Parse USDA response for calories, protein, carbs, fat
- Handle cases where USDA doesn't have exact match (fallback to manual entry)

## Build Configuration

### Dependencies (build.gradle.kts)
```kotlin
// TensorFlow Lite - On-Device ML
implementation("org.tensorflow:tensorflow-lite:2.14.0")
implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")
implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0") // Optional GPU acceleration

// Networking (USDA API only)
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// Lifecycle
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

// Navigation
implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
implementation("androidx.navigation:navigation-ui-ktx:2.7.6")

// Image Loading
implementation("io.coil-kt:coil:2.5.0")

// CameraX
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")

// Material Design
implementation("com.google.android.material:material:1.11.0")
```

## Testing Strategy

### Unit Tests
- Repository layer logic
- ViewModel business logic
- Data transformations
- Date utilities

### Integration Tests
- Room DAO operations
- Repository with fake data sources

### UI Tests
- Fragment navigation
- User interactions
- Data display validation

## Git Workflow
- Never commit `local.properties`
- Include TensorFlow Lite model files in assets (they're part of the app)
- Use meaningful commit messages
- Keep commits atomic and focused

## Security

### Privacy & Data Protection
- **No API Keys Required**: App is completely free and requires no authentication
- **On-Device Processing**: Food detection happens locally - images never leave the device
- **Privacy-First**: User food images are not uploaded anywhere
- **USDA API**: Public API with no authentication, no user data sent

### General Security
- ProGuard rules for release builds
- Input validation for user data
- Secure storage for sensitive data in Room database
- HTTPS only for USDA API requests
- Model file integrity checks on app startup

## Performance
- Use DiffUtil for RecyclerView updates
- Image caching with Coil
- Database queries on background threads
- Pagination for large datasets
- TensorFlow Lite inference on background thread to avoid UI blocking
- Model loaded once and reused for all predictions
- Image preprocessing optimized for speed (resize before normalization)
- Optional GPU delegation for faster inference on supported devices

## Accessibility
- Content descriptions for images
- Proper label associations
- Minimum touch target size (48dp)
- Support for screen readers
