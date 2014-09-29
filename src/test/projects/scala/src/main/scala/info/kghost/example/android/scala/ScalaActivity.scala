package info.kghost.example.android.scala

import android.app.Activity
import android.os.Bundle

class ScalaActivity extends Activity {
  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)
  }
}
