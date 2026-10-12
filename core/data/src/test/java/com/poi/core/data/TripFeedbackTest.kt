package com.poi.core.data
import org.junit.Test
class TripFeedbackTest {
 @Test fun validFeedback() { validateTripFeedback("expenses", 4, "Expense splitting worked well.") }
 @Test(expected = IllegalArgumentException::class) fun invalidRating() { validateTripFeedback("other", 6, "A useful message.") }
 @Test(expected = IllegalArgumentException::class) fun shortMessage() { validateTripFeedback("other", 4, "short") }
 @Test(expected = IllegalArgumentException::class) fun invalidCategory() { validateTripFeedback("private", 4, "A useful message.") }
}
