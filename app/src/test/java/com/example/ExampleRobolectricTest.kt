package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.MobiWaveRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MobiWave", appName)
    }

    @Test
    fun `repository initial data test`() {
        val repo = MobiWaveRepository()
        assertEquals("David Kiprop", repo.managerName)
        assertTrue(repo.expenses.value.isNotEmpty())
        assertTrue(repo.batchRecipients.value.isNotEmpty())

        val initialCount = repo.expenses.value.size
        repo.addExpense(
            merchantName = "TotalEnergies Kilimani",
            amount = 3500.0,
            category = "Transport & Fuel",
            justification = "Emergency generator diesel",
            isOfflineMode = false
        )
        assertEquals(initialCount + 1, repo.expenses.value.size)
    }
}
