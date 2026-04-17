package com.peihua.genai.primitives

enum class FinishCategory {
    /** The response is not finished. */
    notFinished,

    /** The response is finished as completed. */
    completed,

    /** The response is finished as result of interruption. */
    interrupted,
}

class FinishStatus(val category: FinishCategory, details: String? = null) {
    companion object {
        fun notFinished() = FinishStatus(FinishCategory.notFinished)
        fun completed() = FinishStatus(FinishCategory.completed)
        fun interrupted() = FinishStatus(FinishCategory.interrupted)
    }
}