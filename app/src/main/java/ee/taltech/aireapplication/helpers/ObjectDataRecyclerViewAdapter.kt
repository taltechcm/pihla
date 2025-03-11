package ee.taltech.aireapplication.helpers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import ee.taltech.aireapplication.R
import ee.taltech.aireapplication.domain.Location

public interface TObjectDisplayName {
    var displayName: String
}

class ObjectDataRecyclerViewAdapter<TObject: TObjectDisplayName>(
    context: Context,
    private val dataSet: List<TObject>,
    val updateFn: (location: TObject, position: Int) -> Unit,
) : RecyclerView.Adapter<ObjectDataRecyclerViewAdapter<TObject>.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemCount(): Int {
        return dataSet.count()
    }

    private val layoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val rowView = layoutInflater.inflate(R.layout.locations_row, parent, false)
        rowView.layoutParams =
            ViewGroup.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, 128)

        return ViewHolder(rowView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder.itemView.findViewById<Button>(R.id.locationsButtonRow)) {
            text = dataSet[position].displayName
            setOnClickListener {
                updateFn(dataSet[position], position)
            }
        }
    }
}