package com.nexpass.passwordmanager.autofill.matcher

import com.nexpass.passwordmanager.autofill.model.AutofillContext
import com.nexpass.passwordmanager.domain.model.PasswordEntry
import com.nexpass.passwordmanager.domain.repository.PasswordRepository
import java.net.URL

/**
 * Matcher for finding the best password entries for a given autofill context.
 * Implements intelligent matching based on domain, package name, and fuzzy matching.
 */
interface AutofillMatcher {
    /**
     * Find matching password entries for the given autofill context.
     * Returns entries sorted by match score (highest first).
     */
    suspend fun findMatchingEntries(context: AutofillContext): List<PasswordEntry>

    /**
     * Calculate match score for a single entry against the context.
     * Higher scores indicate better matches.
     */
    fun calculateMatchScore(entry: PasswordEntry, context: AutofillContext): Int

    /**
     * Extract the domain from a URL string.
     * e.g., "https://github.com/user/repo" -> "github.com"
     */
    fun extractDomain(url: String): String?
}

/**
 * Default implementation of AutofillMatcher with intelligent scoring.
 */
class AutofillMatcherImpl(
    private val passwordRepository: PasswordRepository
) : AutofillMatcher {

    override suspend fun findMatchingEntries(context: AutofillContext): List<PasswordEntry> {
        // Get all passwords from repository
        val allPasswords = passwordRepository.getAll()

        // Calculate scores for each password
        val scoredEntries = allPasswords.map { entry ->
            ScoredEntry(entry, calculateMatchScore(entry, context))
        }

        // Filter out entries with zero score and sort by score descending
        return scoredEntries
            .filter { it.score > 0 }
            .sortedByDescending { it.score }
            .map { it.entry }
    }

    override fun calculateMatchScore(entry: PasswordEntry, context: AutofillContext): Int {
        var score = 0

        // Score based on package name matching
        context.packageName?.let { packageName ->
            if (entry.packageNames.contains(packageName)) {
                score += MatchScores.EXACT_PACKAGE_NAME
            }
        }

        // Score based on domain matching
        context.webDomain?.let { requestDomain ->
            entry.url?.let { entryUrl ->
                val entryDomain = extractDomain(entryUrl)
                entryDomain?.let { domain ->
                    when {
                        // Exact domain match
                        domain.equals(requestDomain, ignoreCase = true) -> {
                            score += MatchScores.EXACT_DOMAIN
                        }
                        // Subdomain match (e.g., www.github.com vs github.com)
                        isSubdomainMatch(domain, requestDomain) -> {
                            score += MatchScores.SUBDOMAIN
                        }
                        // Root domain match (e.g., api.github.com vs github.com)
                        isSameRootDomain(domain, requestDomain) -> {
                            score += MatchScores.ROOT_DOMAIN
                        }
                        // Fuzzy match (contains keyword)
                        isFuzzyMatch(domain, requestDomain) -> {
                            score += MatchScores.FUZZY_DOMAIN
                        }
                    }
                }
            }
        }

        // Bonus for entries that match both package and domain
        if (context.packageName != null && context.webDomain != null) {
            if (entry.packageNames.contains(context.packageName) &&
                entry.url?.let { extractDomain(it)?.equals(context.webDomain, ignoreCase = true) } == true) {
                score += MatchScores.PACKAGE_AND_DOMAIN_MATCH
            }
        }

        // Small bonus for favorite entries (tie-breaker)
        if (entry.favorite) {
            score += MatchScores.FAVORITE_BONUS
        }

        return score
    }

    override fun extractDomain(url: String): String? {
        return try {
            // Handle URLs without protocol
            val urlWithProtocol = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }

            val parsedUrl = URL(urlWithProtocol)
            parsedUrl.host?.lowercase()
        } catch (e: Exception) {
            // If URL parsing fails, try simple extraction
            extractDomainSimple(url)
        }
    }

    /**
     * Simple domain extraction as fallback.
     */
    private fun extractDomainSimple(url: String): String? {
        return try {
            val cleaned = url
                .removePrefix("http://")
                .removePrefix("https://")
                .split("/")[0]
                .split(":")[0]
                .lowercase()

            if (cleaned.contains(".")) cleaned else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if two domains are subdomain matches.
     * e.g., "www.github.com" and "github.com"
     */
    private fun isSubdomainMatch(domain1: String, domain2: String): Boolean {
        return domain1.endsWith(".$domain2") || domain2.endsWith(".$domain1")
    }

    /**
     * Check if two domains share the same root domain.
     * e.g., "api.github.com" and "github.com"
     */
    private fun isSameRootDomain(domain1: String, domain2: String): Boolean {
        val root1 = getRootDomain(domain1)
        val root2 = getRootDomain(domain2)
        return root1 != null && root2 != null && root1.equals(root2, ignoreCase = true)
    }

    /**
     * Extract root domain from a full domain.
     * e.g., "api.github.com" -> "github.com"
     */
    private fun getRootDomain(domain: String): String? {
        val parts = domain.split(".")
        return if (parts.size >= 2) {
            "${parts[parts.size - 2]}.${parts[parts.size - 1]}"
        } else {
            null
        }
    }

    /**
     * Fuzzy matching for domains that contain similar keywords.
     */
    private fun isFuzzyMatch(domain1: String, domain2: String): Boolean {
        // Extract significant parts (remove common suffixes)
        val name1 = domain1.split(".")[0]
        val name2 = domain2.split(".")[0]

        return name1.contains(name2, ignoreCase = true) ||
               name2.contains(name1, ignoreCase = true)
    }

    private data class ScoredEntry(val entry: PasswordEntry, val score: Int)
}

/**
 * Scoring constants for different match types.
 */
object MatchScores {
    const val EXACT_DOMAIN = 100
    const val EXACT_PACKAGE_NAME = 90
    const val SUBDOMAIN = 80
    const val ROOT_DOMAIN = 70
    const val FUZZY_DOMAIN = 50
    const val PACKAGE_AND_DOMAIN_MATCH = 20
    const val FAVORITE_BONUS = 5
}
