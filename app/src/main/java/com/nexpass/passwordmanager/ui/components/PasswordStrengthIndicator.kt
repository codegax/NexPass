package com.nexpass.passwordmanager.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nexpass.passwordmanager.ui.theme.*

/**
 * Password strength levels
 */
enum class PasswordStrength(
    val label: String,
    val color: Color,
    val progress: Float
) {
    WEAK("Weak", StrengthWeak, 0.25f),
    MEDIUM("Medium", StrengthMedium, 0.5f),
    STRONG("Strong", StrengthStrong, 0.75f),
    VERY_STRONG("Very Strong", StrengthVeryStrong, 1.0f)
}

/**
 * Password strength indicator component
 *
 * Shows visual feedback for password strength with:
 * - Color-coded progress bar
 * - Strength label
 * - Animated transitions
 *
 * @param password The password to evaluate
 * @param modifier Modifier for layout customization
 */
@Composable
fun PasswordStrengthIndicator(
    password: String,
    modifier: Modifier = Modifier
) {
    val strength = calculatePasswordStrength(password)
    val animatedProgress by animateFloatAsState(
        targetValue = strength.progress,
        label = "password_strength_progress"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = strength.color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Strength: ${strength.label}",
            style = MaterialTheme.typography.bodySmall,
            color = strength.color
        )
    }
}

/**
 * Calculate password strength based on multiple criteria
 *
 * Criteria:
 * - Length (minimum 8 chars)
 * - Uppercase letters
 * - Lowercase letters
 * - Numbers
 * - Special characters
 *
 * @param password The password to evaluate
 * @return PasswordStrength enum value
 */
fun calculatePasswordStrength(password: String): PasswordStrength {
    if (password.isEmpty()) {
        return PasswordStrength.WEAK
    }

    var score = 0

    // Length scoring
    when {
        password.length >= 16 -> score += 2
        password.length >= 12 -> score += 1
        password.length >= 8 -> score += 1
    }

    // Character variety scoring
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++

    // Determine strength based on score
    return when {
        score >= 7 -> PasswordStrength.VERY_STRONG
        score >= 5 -> PasswordStrength.STRONG
        score >= 3 -> PasswordStrength.MEDIUM
        else -> PasswordStrength.WEAK
    }
}

/**
 * Password requirements checklist
 *
 * Shows which password requirements are met
 */
@Composable
fun PasswordRequirements(
    password: String,
    modifier: Modifier = Modifier
) {
    val requirements = listOf(
        "At least 8 characters" to (password.length >= 8),
        "Uppercase letter (A-Z)" to password.any { it.isUpperCase() },
        "Lowercase letter (a-z)" to password.any { it.isLowerCase() },
        "Number (0-9)" to password.any { it.isDigit() },
        "Special character (!@#$...)" to password.any { !it.isLetterOrDigit() }
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        requirements.forEach { (requirement, met) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (met) "✓" else "○",
                    color = if (met) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = requirement,
                    color = if (met) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ========== Previews ==========

@Preview(showBackground = true)
@Composable
fun PasswordStrengthIndicatorPreview() {
    NexPassTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PasswordStrengthIndicator(password = "pass")
                PasswordStrengthIndicator(password = "Password1")
                PasswordStrengthIndicator(password = "Password123!")
                PasswordStrengthIndicator(password = "MyStr0ng!Pass@2024")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PasswordRequirementsPreview() {
    NexPassTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Password Requirements",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                PasswordRequirements(password = "MyStr0ng!Pass")
            }
        }
    }
}
