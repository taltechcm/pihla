package ee.taltech.aireapplication.face

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import ee.taltech.aireapplication.R

class FaceDataRecyclerViewAdapter(private val context: Context) :
    RecyclerView.Adapter<FaceDataRecyclerViewAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private val layoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val rowView = layoutInflater.inflate(R.layout.face_row, parent, false)
        rowView.layoutParams =
            ViewGroup.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, 128)

        return ViewHolder(rowView)
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun onBindViewHolder(holder: FaceDataRecyclerViewAdapter.ViewHolder, position: Int) {
        with(holder.itemView.findViewById<Button>(R.id.faceButtonRow)) {
            text =
                dataSet.keys.toList()[position] + " - " + dataSet[dataSet.keys.toList()[position]].toString()
            setOnClickListener {
                deleteFace(dataSet.keys.toList()[position])
            }
        }
    }

    // ============= data source ==========

    fun refreshData() {
        context.contentResolver.refresh(
            Uri.parse("content://com.robotemi.sdk.TemiSdkDocumentContentProvider/face"),
            null,
            null
        )
        queryAllFacesRegistered()
        notifyDataSetChanged()
    }

    private val dataSet: HashMap<String, Int> = HashMap()

    private fun queryAllFacesRegistered(
        selection: String? = null,
        selectionArgs: Array<String>? = null
    ) {
        try {
            dataSet.clear()

            val cursor = context.contentResolver.query(
                Uri.parse("content://com.robotemi.sdk.TemiSdkDocumentContentProvider/face"),
                arrayOf("username", "uid"),
                selection,
                selectionArgs,
                null
            )

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val username = cursor.getString(0)
                    val uid = cursor.getString(1)

                    if (dataSet.containsKey(username)) {
                        dataSet[username] = dataSet[username]!! + 1
                    } else {
                        dataSet[username] = 1
                    }
                }
                cursor.close()
            } else {
                //textViewFaces.text = "face cursor null"
            }

        } catch (e: IllegalArgumentException) {
            Log.e("Query", "Query Exception", e)
        }
    }

    private fun deleteFace(userName: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder
            .setTitle(context.getString(R.string.delete_user))
            .setMessage(context.getString(R.string.delete_user_info_for) + " '$userName'?")
            .setPositiveButton(context.getString(R.string.delete)) { _, _ ->
                context.contentResolver.delete(
                    Uri.parse("content://com.robotemi.sdk.TemiSdkDocumentContentProvider/face"),
                    "username",
                    arrayOf(userName)
                )
                refreshData()
            }
            .setNegativeButton(context.getString(R.string.cancel)) { _, _ ->
            }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

}