package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BangladeshiCustomerFactory
import com.example.ui.AppStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ইস্কাই ব্যাংক", appName)
  }

  @Test
  fun `format BDT currency in Bengali and English`() {
    assertEquals("৳৫,৫০,০০০", AppStrings.formatBdt(550000L, bengali = true))
    assertEquals("৳5,50,000", AppStrings.formatBdt(550000L, bengali = false))
  }

  @Test
  fun `customer factory creates valid Bangladeshi customer request`() {
    val cust = BangladeshiCustomerFactory.nextCustomer(level = 1, carShowroomUnlocked = true, bengali = true)
    assertTrue(cust.portalName.isNotBlank())
    assertTrue(cust.portalAccountNo.startsWith("SKY-"))
    assertTrue(cust.amountBdt > 0L)
  }
}
