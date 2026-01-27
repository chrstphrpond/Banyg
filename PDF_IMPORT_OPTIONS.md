# PDF and Image Import Options for Banyg

## PDF Bank Statement Import

Unlike receipt scanning (OCR), PDF bank statements often contain **structured text** that can be extracted directly without OCR.

### Recommended Libraries

| Library | Version | Pros | Cons |
|---------|---------|------|------|
| **Apache PDFBox** | 3.0.1 | Most popular, mature, free | Larger size (~3MB) |
| **iText** | 8.0.2 | Commercial-grade, fast | Commercial license for non-AGPL projects |
| **Android PdfViewer** | 3.2.0-beta.1 | View + extract text | Limited text extraction |

### Recommendation: Apache PDFBox

**Best choice for Banyg** because:
- ✅ Apache License 2.0 (compatible with proprietary apps)
- ✅ Mature and well-documented
- ✅ Good text extraction from structured documents
- ✅ Handles most bank statement formats

### Usage Example

```kotlin
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.InputStream

class PdfBankStatementParser {
    
    fun extractTransactions(inputStream: InputStream): List<RawTransaction> {
        val transactions = mutableListOf<RawTransaction>()
        
        PDDocument.load(inputStream).use { document ->
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            
            // Parse text line by line
            text.lines().forEach { line ->
                parseTransactionLine(line)?.let { transaction ->
                    transactions.add(transaction)
                }
            }
        }
        
        return transactions
    }
    
    private fun parseTransactionLine(line: String): RawTransaction? {
        // Example pattern matching for bank statements
        // Format: DATE | DESCRIPTION | AMOUNT
        val pattern = """(\d{2}/\d{2}/\d{4})\s+(.+?)\s+([\d,]+\.\d{2})""".toRegex()
        
        return pattern.find(line)?.let { match ->
            RawTransaction(
                date = match.groupValues[1],
                description = match.groupValues[2].trim(),
                amount = match.groupValues[3].replace(",", "").toLong()
            )
        }
    }
}

data class RawTransaction(
    val date: String,
    val description: String,
    val amount: Long
)
```

---

## Image Receipt Support (Post-MVP)

For future receipt scanning feature, you'll need:

### 1. Image Loading
| Library | Version | Purpose |
|---------|---------|---------|
| **Coil** | 2.5.0 | Image loading, caching, display |
| **Glide** | 4.16.0 | Alternative image loader |

**Recommendation: Coil** (Kotlin-first, Compose integration)

```kotlin
import coil.compose.AsyncImage

@Composable
fun ReceiptImage(imageUri: Uri) {
    AsyncImage(
        model = imageUri,
        contentDescription = "Receipt",
        modifier = Modifier.fillMaxWidth()
    )
}
```

### 2. OCR (Text Recognition)
| Library | Purpose | Pros | Cons |
|---------|---------|------|------|
| **ML Kit Text Recognition** | Google's ML Kit | Free, on-device, accurate | Requires Google Play Services |
| **Tesseract** (OpenCV) | Open source OCR | Completely offline, free | Larger size, less accurate |
| **AWS Textract** | Cloud OCR | Very accurate | Requires internet, costs money |

**Recommendation: ML Kit** for best accuracy/ease of use

```kotlin
// ML Kit OCR Example
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ReceiptOcrProcessor {
    private val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )
    
    fun processReceiptImage(image: InputImage): Task<Text> {
        return recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Parse extracted text
                for (block in visionText.textBlocks) {
                    val blockText = block.text
                    // Look for amount patterns: $23.50, 23.50, etc.
                    // Look for date patterns
                    // Look for merchant names
                }
            }
    }
}
```

---

## Implementation Decision Matrix

| Feature | MVP? | Complexity | User Value | Recommendation |
|---------|------|------------|------------|----------------|
| CSV Import | ✅ Yes | Low | High | **Already implemented** |
| PDF Import | ⚠️ Maybe | Medium | Medium | **Add if users request** |
| Receipt Images | ❌ No | Low | Low | Post-MVP |
| Receipt OCR | ❌ No | High | High | Post-MVP |

---

## Suggested Implementation Order

### Phase 1: MVP (Current)
1. ✅ CSV Import (done)

### Phase 2: PDF Support (Optional)
1. Add Apache PDFBox dependency
2. Create `PdfTransactionParser`
3. Add PDF file picker to import flow
4. Extract and preview transactions before import

### Phase 3: Receipts (Post-MVP)
1. Add Coil for image loading
2. Add camera/gallery permission
3. Add receipt attachment to transactions
4. Implement ML Kit OCR
5. Auto-fill transaction from receipt

---

## File Sizes Impact

| Library | Size | Notes |
|---------|------|-------|
| Apache PDFBox | ~2.8MB | Largest, but most capable |
| Coil | ~200KB | Small, efficient |
| ML Kit OCR | ~15MB | Downloaded on demand |

---

## Recommendation

**For now:** Stick with CSV import (already implemented). 

**If users request PDF support:** Add Apache PDFBox for bank statement PDF import (not receipt scanning).

**Post-MVP:** Consider full receipt scanning with ML Kit if budget allows.
