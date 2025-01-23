package ee.taltech.aireapplication.face

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ee.taltech.aireapplication.App.Companion.applicationScope
import ee.taltech.aireapplication.R
import ee.taltech.aireapplication.helpers.BackendApiKtorSingleton
import ee.taltech.aireapplication.helpers.BaseActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class FaceActivity : BaseActivity() {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }


    private lateinit var buttonClose: Button
    private lateinit var imageViewFacePhoto: ImageView
    private lateinit var editTextName: EditText
    private lateinit var buttonAdd: Button
    private lateinit var buttonCancel: Button

    private lateinit var recyclerViewFaces: RecyclerView
    private lateinit var adapter: FaceDataRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face)

        showPrivacyDialog()

        buttonClose = findViewById(R.id.buttonCloseFace)
        imageViewFacePhoto = findViewById(R.id.face_imageView_photo)
        editTextName = findViewById(R.id.face_editText_name)
        recyclerViewFaces = findViewById(R.id.face_recyclerView_faces)
        buttonAdd = findViewById(R.id.face_button_add)
        buttonCancel = findViewById(R.id.face_button_cancel)

        editTextName.setText("")

        buttonAdd.isEnabled = false
        buttonCancel.isEnabled = false
        editTextName.isEnabled = false

        recyclerViewFaces.layoutManager = LinearLayoutManager(this)
        adapter = FaceDataRecyclerViewAdapter(this)
        adapter.refreshData()
        recyclerViewFaces.adapter = adapter

    }

    private fun closeActivity() {
        app.robot.cancelAllTtsRequests()

        buttonClose.text = getString(R.string.Close)

        app.robot.startFaceRecognition(withSdkFaces = true)


        applicationScope.launch {
            BackendApiKtorSingleton.logEvent(
                tag = "$TAG.button", message = "closeButtonOnClick"
            )
        }
        finish()
    }

    fun buttonCloseFaceOnClick(view: View) {
        closeActivity()
    }


    override fun onResume() {
        super.onResume()
        app.robot.stopFaceRecognition()
    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data

                @Suppress("DEPRECATION")
                val imageBitmap = intent?.extras?.get("data") as? Bitmap

                if (imageBitmap != null) {
                    val drawable = BitmapDrawable(resources, imageBitmap)
                    imageViewFacePhoto.setImageDrawable(drawable)

                    // save the photo
                    val file = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "face.jpg"
                    )
                    file.createNewFile()
                    val fileOutputStream = FileOutputStream(file)
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream)
                    fileOutputStream.close()
                    Log.d(TAG, "Face image file saved")

                    buttonAdd.isEnabled = true
                    buttonCancel.isEnabled = true
                    editTextName.isEnabled = true
                }
            }
        }

    fun buttonFaceTakePictureOnClick(view: View) {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startForResult.launch(takePictureIntent)
        }

    }


    fun buttonAddOnclick(view: View) {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "face.jpg"
        )

        val fileUri = file.toUri()
        val name = editTextName.text.toString().trim().uppercase()

        val contentValues = ContentValues()
        contentValues.put("uid", name)
        contentValues.put("username", name)
        contentValues.put("uri", fileUri.toString())


        applicationScope.launch {
            contentResolver.insert(
                Uri.parse("content://com.robotemi.sdk.TemiSdkDocumentContentProvider/face"),
                contentValues
            )

            delay(2000)

            adapter.refreshData()
        }

        disableAddCancel(view)
    }

    fun buttonCancelOnClick(view: View) {
        disableAddCancel(view)
    }

    private fun disableAddCancel(view: View) {
        imageViewFacePhoto.setImageResource(R.drawable.pihlakodu)
        buttonCancel.isEnabled = false
        buttonAdd.isEnabled = false

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showPrivacyDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.privacy_dialog_title)
            .setMessage(R.string.privacy_dialog_message)
            .setPositiveButton(R.string.agree) { _, _ ->
                // User agreed, continue with activity
            }
            .setNegativeButton(R.string.disagree) { _, _ ->
                // User disagreed, return to previous activity
                finish()
            }
            .setCancelable(false)  // Prevent dismissing by clicking outside
            .show()
    }

}