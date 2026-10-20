package io.github.vadimtoptunov.kassitestdata.action

import io.github.vadimtoptunov.kassi.review.ReviewPrompt

/** The one-time Marketplace review nudge for this plugin (rules: ReviewPromptPolicy). */
internal val reviewPrompt = ReviewPrompt(
    pluginKey = "testdata",
    pluginName = "Kassi Test Data",
    reviewUrl = "https://plugins.jetbrains.com/plugin/33149/reviews",
    notificationGroupId = "Kassi Test Data",
)
