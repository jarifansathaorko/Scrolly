package com.example.data.repository

/**
 * The live "scrolls today" number shown by the dashboard and the floating island.
 *
 * It is modelled as `value = stored + pending`:
 *  - `stored`  – the last total known to be persisted for [date].
 *  - `pending` – optimistic increments whose database write has not finished yet.
 *
 * Rules, each of which fixes a defect in the previous "only ever accept increases" approach:
 *  1. **Failed writes roll back.** [rollback] removes exactly that write's optimistic delta, so one
 *     failed insert can no longer inflate the counter until the process dies.
 *  2. **Day changes reset it.** The first [add] or [anchor] for a newer date starts that day from
 *     its stored total, so the counter no longer keeps climbing from yesterday's total.
 *  3. **Older dates never move it.** A straggler for an earlier date (event stamped just before
 *     midnight, processed just after) is persisted by the caller but cannot drag the live counter
 *     back to yesterday. Date keys are ISO `yyyy-MM-dd`, so string order is chronological.
 *  4. **No transient double count.** [anchor] (database emissions) is ignored while writes for the
 *     current date are still in flight; [commit] then applies the authoritative total.
 *
 * All members are synchronized; callers publish [value] to a StateFlow while holding no other lock.
 */
class DailyCounter {

    private var date: String = ""
    private var stored: Int = 0
    private var pending: Int = 0

    @get:Synchronized
    val value: Int
        get() = stored + pending

    @get:Synchronized
    val currentDate: String
        get() = date

    /** Optimistically adds [delta] scrolls for [forDate] and returns the new value. */
    @Synchronized
    fun add(forDate: String, delta: Int): Int {
        if (delta <= 0) return value
        if (forDate < date) return value // rule 3
        if (forDate != date) { // rule 2
            date = forDate
            stored = 0
            pending = 0
        }
        pending += delta
        return value
    }

    /** The write for [delta] scrolls on [forDate] succeeded and the database now holds [newTotal]. */
    @Synchronized
    fun commit(forDate: String, delta: Int, newTotal: Int): Int {
        if (forDate == date) {
            stored = newTotal
            pending = (pending - delta).coerceAtLeast(0)
        }
        return value
    }

    /** The write for [delta] scrolls on [forDate] failed or was cancelled; undo its optimistic add. */
    @Synchronized
    fun rollback(forDate: String, delta: Int): Int {
        if (forDate == date) {
            pending = (pending - delta).coerceAtLeast(0) // rule 1
        }
        return value
    }

    /** The database reports [total] for [forDate]. Returns the new value. */
    @Synchronized
    fun anchor(forDate: String, total: Int): Int {
        when {
            forDate < date -> Unit // rule 3
            forDate != date -> { // rule 2
                date = forDate
                stored = total.coerceAtLeast(0)
                pending = 0
            }
            pending == 0 -> stored = total.coerceAtLeast(0) // rule 4
        }
        return value
    }
}
