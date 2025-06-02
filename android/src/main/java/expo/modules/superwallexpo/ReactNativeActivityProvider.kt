package expo.modules.superwallexpo

import android.app.Activity
import expo.modules.kotlin.AppContext
import com.superwall.sdk.misc.ActivityProvider

class ExpoActivityProvider(private val appContext: AppContext) : ActivityProvider {

  override fun getCurrentActivity(): Activity? {
    return appContext.currentActivity
  }
}
