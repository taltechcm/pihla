package ee.taltech.aireapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import ee.taltech.aireapplication.R
import ee.taltech.aireapplication.helpers.BaseActivity

class TestActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)


    }


}