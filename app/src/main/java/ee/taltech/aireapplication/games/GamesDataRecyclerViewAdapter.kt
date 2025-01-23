package ee.taltech.aireapplication.games

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import ee.taltech.aireapplication.R
import ee.taltech.aireapplication.domain.Location
import ee.taltech.aireapplication.dto.WebLink

class GamesDataRecyclerViewAdapter(
    context: Context,
    var dataSet: List<WebLink>,
    val updateFn: (selectedItem: WebLink, position: Int) -> Unit,
) :
    RecyclerView.Adapter<GamesDataRecyclerViewAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private val layoutInflater = LayoutInflater.from(context)


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GamesDataRecyclerViewAdapter.ViewHolder {
        val rowView = layoutInflater.inflate(R.layout.locations_row, parent, false)
        rowView.layoutParams =
            ViewGroup.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, 128)

        return ViewHolder(rowView)
    }

    override fun getItemCount(): Int {
        return dataSet.count()
    }

    override fun onBindViewHolder(holder: GamesDataRecyclerViewAdapter.ViewHolder, position: Int) {
        with(holder.itemView.findViewById<Button>(R.id.locationsButtonRow)) {
            text = dataSet[position].webLinkDisplayName
            setOnClickListener {
                updateFn(dataSet[position], position)
            }
        }
    }

}