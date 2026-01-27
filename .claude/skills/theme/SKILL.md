---
name: theme
description: Generate Material 3 theme code with Banyg branding from token JSON or simple palette
disable-model-invocation: true
---

# Theme Generator

Generate a complete Material 3 theme implementation with Banyg branding guidelines.

## Input

Accept one of:
- Token JSON with color definitions
- Simple palette (primary, secondary, tertiary, etc.)
- Color values in hex or standard format

## Output

Generate the following Kotlin/Compose code:

### 1. ColorScheme
```kotlin
// core/ui/theme/Color.kt
val BanygLightColors = lightColorScheme(
    primary = ...,
    onPrimary = ...,
    // ... complete Material 3 color scheme
)

val BanygDarkColors = darkColorScheme(
    primary = ...,
    onPrimary = ...,
    // ... complete Material 3 color scheme
)
```

### 2. Typography
```kotlin
// core/ui/theme/Type.kt
val BanygTypography = Typography(
    displayLarge = TextStyle(...),
    // Use modern sans, 2-3 weights max
    // Avoid decorative fonts
)
```

### 3. Shape System
```kotlin
// core/ui/theme/Shape.kt
val BanygShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)
```

### 4. Gradients
```kotlin
// core/ui/theme/Gradient.kt
object BanygGradients {
    val subtleGlow = Brush.linearGradient(...)
    val wovenRhythm = Brush.linearGradient(...)
    // Calm, soft gradients only
}
```

### 5. Spacing Constants
```kotlin
// core/ui/theme/Spacing.kt
object BanygSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}
```

### 6. Theme Composable
```kotlin
// core/ui/theme/Theme.kt
@Composable
fun BanygTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BanygDarkColors else BanygLightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BanygTypography,
        shapes = BanygShapes,
        content = content
    )
}
```

## Branding Rules (Banyg)

**Enforce these constraints:**
- No obvious flags or literal cultural symbols
- Subtle cues only: woven rhythm, soft glow, calm gradients
- Modern sans typography, 2-3 weights maximum
- Calm, premium feel
- No bright, harsh colors
- Motion should be short and calm (note this in animation constants if relevant)

## Workflow

1. Parse the input palette/tokens
2. Generate complete Material 3 color scheme (light and dark)
3. Create typography with limited weight variations
4. Define shape system with consistent corner radii
5. Add branded gradients (2-3 maximum, subtle)
6. Define spacing scale
7. Create theme composable that ties it all together
8. Provide usage example in a sample screen

## Complete Color Scheme Example

### Light Theme
```kotlin
val BanygLightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),

    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),

    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),

    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),

    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),

    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)
```

### Dark Theme
```kotlin
val BanygDarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),

    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),

    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),

    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),

    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),

    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)
```

## Animation Constants

```kotlin
// core/ui/theme/Motion.kt
object BanygMotion {
    // Calm, short durations - no bounce
    const val DURATION_SHORT = 150
    const val DURATION_MEDIUM = 250
    const val DURATION_LONG = 300

    // Easing - gentle, not bouncy
    val EASING_STANDARD = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    val EASING_ENTER = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val EASING_EXIT = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
}

// Usage in animations
@Composable
fun AnimatedExample() {
    val scale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.95f,
        animationSpec = tween(
            durationMillis = BanygMotion.DURATION_MEDIUM,
            easing = BanygMotion.EASING_STANDARD
        )
    )
}
```

## Example Usage

### Basic Screen with Theme
```kotlin
@Preview(name = "Light Theme")
@Composable
fun ThemePreviewLight() {
    BanygTheme(darkTheme = false) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.padding(BanygSpacing.md),
                verticalArrangement = Arrangement.spacedBy(BanygSpacing.sm)
            ) {
                Text(
                    text = "Display Large",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Headline Medium",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Body Large - The quick brown fox jumps over the lazy dog.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Card with gradient
                GradientCard(
                    title = "Account Balance",
                    subtitle = "$1,234.56",
                    gradient = BanygGradients.subtleGlow
                )
            }
        }
    }
}

@Preview(name = "Dark Theme")
@Composable
fun ThemePreviewDark() {
    BanygTheme(darkTheme = true) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.padding(BanygSpacing.md),
                verticalArrangement = Arrangement.spacedBy(BanygSpacing.sm)
            ) {
                Text(
                    text = "Display Large",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Body Large",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

### Color Showcase
```kotlin
@Preview
@Composable
fun ColorShowcase() {
    BanygTheme {
        LazyColumn(
            contentPadding = PaddingValues(BanygSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BanygSpacing.sm)
        ) {
            item {
                ColorSwatch("Primary", MaterialTheme.colorScheme.primary)
                ColorSwatch("Secondary", MaterialTheme.colorScheme.secondary)
                ColorSwatch("Tertiary", MaterialTheme.colorScheme.tertiary)
                ColorSwatch("Error", MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ColorSwatch(name: String, color: Color) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        color = color,
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier.padding(BanygSpacing.md),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}
```

## Verification Checklist

After generating the theme, verify:

- [ ] Both light and dark color schemes are complete (all Material 3 colors defined)
- [ ] Typography uses modern sans serif, 2-3 weights maximum
- [ ] Shapes have consistent corner radii
- [ ] Gradients are subtle and calm (not harsh or bright)
- [ ] Spacing scale is consistent and follows 4dp/8dp grid
- [ ] Animation durations are short (150-300ms)
- [ ] No bouncy easing curves
- [ ] Theme composable ties everything together
- [ ] Preview composables demonstrate light and dark modes
- [ ] Branding feels premium and calm, not flashy

## Usage

```bash
# Invoke the skill with palette/tokens
/theme

# Provide color values:
# - Hex colors: #6750A4
# - Named colors: Material Purple 500
# - Token JSON: { "primary": "#6750A4", ... }
```

## Output Location

All generated files should go in:
```
core/ui/theme/
├── Color.kt
├── Type.kt
├── Shape.kt
├── Spacing.kt
├── Gradient.kt
├── Motion.kt
└── Theme.kt
```
