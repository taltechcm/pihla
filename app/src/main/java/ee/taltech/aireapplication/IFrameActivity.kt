package ee.taltech.aireapplication

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import ee.taltech.aireapplication.App.Companion.applicationScope
import ee.taltech.aireapplication.helpers.BackendApiKtorSingleton
import ee.taltech.aireapplication.helpers.BaseActivity
import ee.taltech.aireapplication.helpers.C
import kotlinx.coroutines.launch

class IFrameActivity : BaseActivity() {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iframe)

        val html = intent
            .getStringExtra(C.EXTRA_WEBVIEW_LAUNCH_URL)!!


        val myWebView: WebView = findViewById(R.id.articleVebview)
        myWebView.settings.javaScriptEnabled = true


        //"scrolling=\"no\""

        myWebView.loadData(html, "text/html", null)
    }

    fun closeButtonOnClick(view: View) {
        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "$TAG.button", message = "closeButtonOnClick"
            )
        }

        finish()
    }
}
