package io.github.salihoguz.minlauncher.domain

/** Rules for user folders. Apps can be in one folder or none. */
object Folders {
    const val MAX_NAME_LENGTH = 24

    /**
     * Android's own app categories (ApplicationInfo.CATEGORY_*), as plain numbers so this stays
     * testable without Android.
     */
    private val suggestionByCategory = mapOf(
        0 to "Games", // CATEGORY_GAME
        1 to "Media", // CATEGORY_AUDIO
        2 to "Media", // CATEGORY_VIDEO
        3 to "Media", // CATEGORY_IMAGE
        4 to "Social", // CATEGORY_SOCIAL
        5 to "News", // CATEGORY_NEWS
        6 to "Travel", // CATEGORY_MAPS
        7 to "Work", // CATEGORY_PRODUCTIVITY
    )

    private val categoryNames = mapOf(
        0 to "Game", 1 to "Audio", 2 to "Video", 3 to "Image",
        4 to "Social", 5 to "News", 6 to "Maps", 7 to "Productivity",
    )

    /** What Android calls the category ("Maps"), or null when the app doesn't say. */
    fun categoryName(category: Int): String? = categoryNames[category]

    /**
     * A folder to suggest for an app with this category. Reuses an existing folder with the same
     * name in any capitalisation; otherwise suggests a new one.
     */
    fun suggestion(category: Int, existing: List<String>): String? {
        val name = suggestionByCategory[category] ?: return null
        return existing.firstOrNull { it.equals(name, ignoreCase = true) } ?: name
    }

    /** Why [name] can't be used for a folder, or null when it can. */
    fun problemWithName(name: String, existing: List<String>, renaming: String? = null): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "Type a name first."
            trimmed.length > MAX_NAME_LENGTH -> "Keep it under $MAX_NAME_LENGTH letters."
            existing.any { it.equals(trimmed, ignoreCase = true) && !it.equals(renaming, ignoreCase = true) } ->
                "A folder called $trimmed already exists."
            else -> null
        }
    }
}
