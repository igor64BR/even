package com.rateio.app.ui.createexpense

/**
 * The active tab in "How to split" (the prototype's `#tabs`: Equal/Percentage/Fixed amount) —
 * purely a form UI detail, not to be confused with the domain hierarchy
 * [ExpenseSplit][com.rateio.domain.model.ExpenseSplit]. There, deliberately, there's no separate
 * enum (see the comment in `ExpenseSplit.kt`: the concrete subtype already carries the split rule,
 * a separate enum could diverge from each split's real subtype). Here it's different: before
 * saving, the screen only has *one* set of participants with inputs per mode (checkbox / % / R$) —
 * something is needed to know which set of fields to render and validate, and this never
 * persists — it's translated into the right [com.rateio.domain.model.ExpenseSplit] subtype only at
 * save time (see `CreateExpenseViewModel.buildSplits`).
 */
enum class SplitMode {
    EQUAL,
    PERCENTAGE,
    FIXED_AMOUNT,
}
