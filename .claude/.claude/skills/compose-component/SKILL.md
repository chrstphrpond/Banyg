---
name: compose-component
description: Scaffold reusable Compose UI components that match Banyg design system
disable-model-invocation: true
---

# Compose Component Generator

Generate production-ready Compose components that follow Banyg design system conventions.

## Supported Components

- GradientCard
- PillButton
- IconButton
- BottomNav
- ChartPanel
- InputField
- AmountInput
- CategoryChip
- TransactionRow
- Custom components as requested

## Output Structure

For each component, generate:

### 1. Component File
```kotlin
// core/ui/components/[ComponentName].kt

/**
 * [Brief description of component purpose]
 */
@Composable
fun ComponentName(
    // Required parameters first
    text: String,
    onClick: () -> Unit,
    // Optional parameters with defaults
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    // ... other parameters
) {
    // Stateless implementation
    // Use Material 3 components as base
    // Apply Banyg theme tokens
}
```

### 2. Variants (if applicable)
```kotlin
@Composable
fun ComponentNameSmall(...) { /* size variant */ }

@Composable
fun ComponentNameLarge(...) { /* size variant */ }
```

### 3. Preview Composables
```kotlin
@Preview(name = "Light Mode")
@Composable
private fun ComponentNamePreview() {
    BanygTheme {
        Surface {
            ComponentName(
                text = "Preview",
                onClick = {}
            )
        }
    }
}

@Preview(name = "Dark Mode")
@Composable
private fun ComponentNameDarkPreview() {
    BanygTheme(darkTheme = true) {
        Surface {
            ComponentName(
                text = "Preview",
                onClick = {}
            )
        }
    }
}

@Preview(name = "Disabled State")
@Composable
private fun ComponentNameDisabledPreview() {
    BanygTheme {
        Surface {
            ComponentName(
                text = "Preview",
                onClick = {},
                enabled = false
            )
        }
    }
}
```

## Requirements

**Always include:**
- Stateless composable (hoist state up)
- `Modifier` parameter (always last or first in optional params)
- Material 3 theming (use `MaterialTheme.colorScheme`, `MaterialTheme.typography`)
- Accessibility:
  - Proper semantics for screen readers
  - Minimum touch target size (48.dp)
  - Sufficient color contrast
  - Content descriptions where needed
- Multiple preview composables (light, dark, disabled, error states)

**Follow Banyg conventions:**
- Use design tokens from `BanygSpacing`, `BanygGradients`, etc.
- Calm motion (short duration, no bounce)
- Premium feel, not flashy
- Handle edge cases (long text, empty states)

## Example Components

### GradientCard
```kotlin
@Composable
fun GradientCard(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    gradient: Brush = BanygGradients.subtleGlow
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(BanygSpacing.md)
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                subtitle?.let {
                    Spacer(modifier = Modifier.height(BanygSpacing.xs))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
```

### AmountInput (money-aware)
```kotlin
@Composable
fun AmountInput(
    amountMinor: Long,
    onAmountChange: (Long) -> Unit,
    currency: Currency,
    modifier: Modifier = Modifier,
    label: String = "Amount",
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    placeholder: String = "0.00"
) {
    // State for text input (formatted as user types)
    var textValue by remember(amountMinor) {
        mutableStateOf(formatAmountMinor(amountMinor, currency))
    }
    var isFocused by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = textValue,
        onValueChange = { newText ->
            // Parse input and convert to Long (minor units)
            val cleanedText = newText.filter { it.isDigit() || it == '.' || it == ',' }

            // Try to parse as Long minor units
            val parsedAmount = parseAmountToMinor(cleanedText, currency)

            if (parsedAmount != null) {
                textValue = cleanedText
                onAmountChange(parsedAmount)
            }
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        isError = isError,
        supportingText = errorMessage?.let { { Text(it) } },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                // Format final value on blur
                textValue = formatAmountMinor(amountMinor, currency)
            }
        ),
        leadingIcon = {
            Text(
                text = currency.symbol,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            textAlign = TextAlign.End
        )
    )

    // Format on blur
    LaunchedEffect(isFocused) {
        if (!isFocused && textValue.isNotEmpty()) {
            textValue = formatAmountMinor(amountMinor, currency)
        }
    }
}

/**
 * Format amountMinor (Long) to user-friendly string.
 * Example: 123456L -> "1,234.56"
 */
private fun formatAmountMinor(amountMinor: Long, currency: Currency): String {
    val isNegative = amountMinor < 0
    val absoluteAmount = kotlin.math.abs(amountMinor)

    // Convert minor units to major units (cents to dollars)
    val major = absoluteAmount / 100
    val minor = absoluteAmount % 100

    // Format with thousands separator
    val formattedMajor = major.toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()

    val sign = if (isNegative) "-" else ""
    return "$sign$formattedMajor.${minor.toString().padStart(2, '0')}"
}

/**
 * Parse user input string to Long minor units.
 * Example: "1,234.56" -> 123456L
 * Returns null if invalid input.
 */
private fun parseAmountToMinor(text: String, currency: Currency): Long? {
    if (text.isBlank()) return 0L

    return try {
        // Remove commas and other formatting
        val cleaned = text.replace(",", "").replace(" ", "")

        // Split on decimal point
        val parts = cleaned.split(".")

        when (parts.size) {
            1 -> {
                // No decimal point: "1234" -> 123400L
                val major = parts[0].toLongOrNull() ?: return null
                major * 100
            }
            2 -> {
                // Has decimal: "1234.56" -> 123456L
                val major = parts[0].toLongOrNull() ?: 0L
                val minorStr = parts[1].take(2).padEnd(2, '0')
                val minor = minorStr.toLongOrNull() ?: return null
                (major * 100) + minor
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}
```

### TransactionRow
```kotlin
@Composable
fun TransactionRow(
    merchant: String,
    amountMinor: Long,
    currency: Currency,
    date: String,
    category: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(BanygSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = merchant,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(BanygSpacing.xs))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(BanygSpacing.xs)
                ) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    category?.let {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(BanygSpacing.md))

            // Amount with color based on sign
            Text(
                text = formatAmountMinor(amountMinor, currency),
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    amountMinor < 0 -> MaterialTheme.colorScheme.error  // Expense
                    amountMinor > 0 -> Color(0xFF2E7D32)  // Income (green)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant  // Zero
                }
            )
        }
    }
}
```

## Workflow

1. Understand the component requirements
2. Check if similar component exists in core/ui/components/
3. Generate stateless composable with proper parameters
4. Apply Material 3 + Banyg theme tokens
5. Add accessibility semantics
6. Create comprehensive previews
7. If money-related, enforce Long for minor units
8. Provide usage example
