# Contributing to NexPass

Thank you for considering contributing to NexPass! We welcome contributions from the community to help make NexPass the best privacy-focused password manager for Android.

## Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inclusive environment for all contributors, regardless of experience level, gender identity, sexual orientation, disability, personal appearance, race, ethnicity, age, religion, or nationality.

### Expected Behavior

- Be respectful and constructive in discussions
- Welcome newcomers and help them get started
- Focus on what is best for the community and project
- Show empathy towards other community members
- Provide and accept constructive feedback gracefully

### Unacceptable Behavior

- Harassment, discrimination, or offensive comments
- Personal attacks or trolling
- Publishing others' private information
- Any conduct that would be inappropriate in a professional setting

### Enforcement

Violations of the code of conduct can be reported to [conduct@daguva.com](mailto:conduct@daguva.com). All complaints will be reviewed and investigated promptly and fairly.

## How Can I Contribute?

### Reporting Bugs

Before creating a bug report:

1. **Check existing issues** to avoid duplicates
2. **Use the latest version** to ensure the bug hasn't been fixed
3. **Gather information** about the bug

When reporting a bug, include:

- **Clear title** describing the issue
- **Steps to reproduce** the behavior
- **Expected behavior** vs. actual behavior
- **Screenshots** if applicable
- **Device information**:
  - Android version
  - Device manufacturer and model
  - NexPass version
- **Logs** if available (Settings → About → Export Logs)

Use our [Bug Report Template](.github/ISSUE_TEMPLATE/bug_report.md).

### Suggesting Features

We love feature suggestions! Before creating a feature request:

1. **Check existing issues** for similar requests
2. **Consider the scope** - does it fit NexPass's goals?
3. **Think about privacy** - does it maintain zero-knowledge principles?

When suggesting a feature, include:

- **Clear title** describing the feature
- **Use case** - what problem does it solve?
- **Proposed solution** - how should it work?
- **Alternatives considered** - what other solutions did you think about?
- **Mockups or examples** if applicable

Use our [Feature Request Template](.github/ISSUE_TEMPLATE/feature_request.md).

### Submitting Pull Requests

#### Before You Start

1. **Discuss major changes** in an issue first
2. **Check existing PRs** to avoid duplicate work
3. **Ensure you can build** the project successfully

#### Development Setup

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/nexpass.git
cd nexpass

# Add upstream remote
git remote add upstream https://github.com/codegax/nexpass.git

# Create a feature branch
git checkout -b feature/amazing-feature

# Build and test
./gradlew assembleDebug
./gradlew test
```

#### Development Workflow

1. **Fork the repository** on GitHub
2. **Create a feature branch** from `main`
   - Feature: `feature/add-password-history`
   - Bug fix: `fix/autofill-crash`
   - Documentation: `docs/update-readme`
3. **Make your changes** following our coding standards
4. **Write/update tests** for your changes
5. **Test thoroughly** on a real device or emulator
6. **Commit your changes** with clear messages
7. **Push to your fork** and create a Pull Request

#### Pull Request Guidelines

- **Title**: Clear, descriptive, prefixed with type
  - `feat: Add password history feature`
  - `fix: Resolve autofill crash on Android 12`
  - `docs: Update Nextcloud setup guide`
  - `refactor: Simplify crypto manager implementation`
  - `test: Add unit tests for folder repository`

- **Description**: Use the [PR template](.github/PULL_REQUEST_TEMPLATE.md)
  - What does this PR do?
  - Why is this change needed?
  - How was it tested?
  - Screenshots (for UI changes)
  - Related issues (closes #123)

- **Code Quality**:
  - Follows Kotlin coding conventions
  - No linter warnings (`./gradlew lint`)
  - Tests pass (`./gradlew test`)
  - Code is well-documented

- **Small, Focused PRs**: Easier to review and merge
  - One feature/fix per PR
  - Avoid mixing refactoring with features
  - Split large features into multiple PRs

## Coding Standards

### Kotlin Style Guide

We follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

#### Naming Conventions

```kotlin
// Classes: PascalCase
class PasswordRepository

// Functions and properties: camelCase
fun encryptPassword()
val userPassword: String

// Constants: UPPER_SNAKE_CASE
const val MAX_PASSWORD_LENGTH = 256

// Private properties: camelCase with leading underscore (optional)
private val _passwords = MutableStateFlow<List<Password>>(emptyList())
val passwords: StateFlow<List<Password>> = _passwords.asStateFlow()
```

#### Code Formatting

- **Indentation**: 4 spaces (no tabs)
- **Line length**: Max 120 characters
- **Braces**: K&R style (opening brace on same line)

```kotlin
// Good
fun validatePassword(password: String): Boolean {
    if (password.isEmpty()) {
        return false
    }
    return password.length >= 8
}

// Bad
fun validatePassword(password: String): Boolean
{
    if (password.isEmpty())
    {
        return false
    }
    return password.length >= 8
}
```

#### Documentation

- **Public APIs**: Always document with KDoc

```kotlin
/**
 * Encrypts a password using AES-256-GCM encryption.
 *
 * @param plaintext The password to encrypt in plaintext
 * @param key The encryption key (must be 256 bits)
 * @return Encrypted data containing ciphertext, IV, and tag
 * @throws EncryptionException if encryption fails
 */
fun encryptPassword(plaintext: String, key: ByteArray): EncryptedData
```

- **Complex logic**: Add inline comments explaining "why", not "what"

```kotlin
// Good: Explains reasoning
// Use PBKDF2 with 100k iterations to resist brute-force attacks
val derivedKey = deriveKey(masterPassword, salt, iterations = 100_000)

// Bad: Describes what code does (obvious)
// Call deriveKey function
val derivedKey = deriveKey(masterPassword, salt, iterations = 100_000)
```

### Architecture Patterns

NexPass follows **Clean Architecture** + **MVVM**:

#### Layer Structure

```
domain/           # Business logic (pure Kotlin, no Android deps)
├── model/        # Domain models
├── repository/   # Repository interfaces
└── usecase/      # Use cases

data/             # Data layer (Android-specific)
├── local/        # Room database, DAOs
├── network/      # API clients
└── repository/   # Repository implementations

ui/               # Presentation layer
├── screens/      # Composable screens
├── viewmodel/    # ViewModels (state management)
└── components/   # Reusable UI components
```

#### Dependency Flow

```
UI → ViewModel → UseCase/Repository → Data Source
```

- **Domain layer** has no dependencies on Android or data layer
- **Data layer** depends on domain (implements interfaces)
- **UI layer** depends on domain (ViewModels use use cases/repositories)

#### Example Implementation

```kotlin
// Domain: Repository interface (domain/repository/)
interface PasswordRepository {
    suspend fun getAllPasswords(): Result<List<PasswordEntry>>
    suspend fun createPassword(password: PasswordEntry): Result<Unit>
}

// Data: Repository implementation (data/repository/)
class PasswordRepositoryImpl(
    private val passwordDao: PasswordDao,
    private val cryptoManager: CryptoManager
) : PasswordRepository {
    override suspend fun getAllPasswords(): Result<List<PasswordEntry>> {
        // Implementation
    }
}

// UI: ViewModel (ui/viewmodel/)
class VaultListViewModel(
    private val passwordRepository: PasswordRepository
) : ViewModel() {
    // State and logic
}
```

### Testing Requirements

#### Unit Tests Required For

- All repository methods
- All use cases
- All ViewModels (state transitions)
- Crypto operations (critical!)
- Utility functions

#### Test Coverage Goals

- **Security layer**: 90%+ (critical for safety)
- **Overall**: 70%+ minimum

#### Writing Tests

```kotlin
class PasswordRepositoryTest {
    private lateinit var repository: PasswordRepositoryImpl
    private lateinit var mockDao: PasswordDao

    @Before
    fun setup() {
        mockDao = mockk()
        repository = PasswordRepositoryImpl(mockDao, mockCrypto)
    }

    @Test
    fun `getAllPasswords returns decrypted passwords`() = runTest {
        // Given
        val encryptedPassword = createMockEncryptedPassword()
        whenever(mockDao.getAllPasswords()).thenReturn(listOf(encryptedPassword))

        // When
        val result = repository.getAllPasswords()

        // Then
        assertTrue(result.isSuccess)
        assertEquals("decrypted", result.getOrNull()?.first()?.password)
    }
}
```

### Security Best Practices

When contributing code that handles sensitive data:

1. **Never log sensitive information**

```kotlin
// Bad
Log.d("Auth", "Master password: $masterPassword")

// Good
Log.d("Auth", "Master password validation completed")
```

2. **Wipe sensitive data from memory**

```kotlin
// Good
val passwordBytes = password.toByteArray()
try {
    // Use passwordBytes
} finally {
    MemoryUtils.wipeByteArray(passwordBytes)
}
```

3. **Use secure random for cryptographic operations**

```kotlin
// Good
val random = SecureRandom()

// Bad
val random = Random()
```

4. **Validate all inputs**

```kotlin
fun setMasterPassword(password: String) {
    require(password.isNotBlank()) { "Password cannot be blank" }
    require(password.length >= 8) { "Password must be at least 8 characters" }
    // Proceed with secure operations
}
```

## Git Commit Messages

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- **feat**: New feature
- **fix**: Bug fix
- **docs**: Documentation changes
- **style**: Formatting (no code change)
- **refactor**: Code refactoring
- **test**: Adding tests
- **chore**: Build/tooling changes

### Examples

```
feat(autofill): Add fuzzy matching for subdomain detection

Implements fuzzy matching algorithm that matches login.example.com
with example.com credentials. Uses Levenshtein distance with 80%
similarity threshold.

Closes #42
```

```
fix(crypto): Resolve memory leak in key derivation

ByteArray instances were not being properly wiped after use, causing
sensitive data to remain in memory. Now uses try-finally blocks with
explicit wiping.

Fixes #87
```

## Review Process

### What We Look For

- **Functionality**: Does it work as expected?
- **Code quality**: Is it clean, readable, maintainable?
- **Tests**: Are there adequate tests?
- **Security**: Does it maintain privacy/security standards?
- **Performance**: Is it efficient?
- **Documentation**: Is it well-documented?

### Timeline

- **Initial review**: Within 7 days
- **Follow-up reviews**: Within 3 days
- **Merge**: After approval from at least one maintainer

### Addressing Feedback

- Respond to all review comments
- Mark conversations as resolved when addressed
- Push new commits (don't force-push during review)
- Request re-review when ready

## Release Process

We follow [Semantic Versioning](https://semver.org/):

- **Major (1.0.0)**: Breaking changes
- **Minor (0.1.0)**: New features (backward compatible)
- **Patch (0.0.1)**: Bug fixes (backward compatible)

Releases are managed by maintainers through GitHub Actions.

## Getting Help

- **Questions**: Open a [Discussion](https://github.com/codegax/nexpass/discussions)
- **Email**: [dev@daguva.com](mailto:dev@daguva.com)

## Recognition

Contributors are recognized in:

- README acknowledgments
- Release notes
- GitHub contributors page

Thank you for helping make NexPass better!

---

**License Note**: By contributing to NexPass, you agree that your contributions will be licensed under the GNU General Public License v3.0 (GPLv3).
