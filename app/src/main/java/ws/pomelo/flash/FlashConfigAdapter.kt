package ws.pomelo.flash

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView

class FlashConfigAdapter(
    private var configs: List<FlashConfig>,
    private val onToggle: (FlashConfig, Boolean) -> Unit,
    private val onClick: (FlashConfig) -> Unit
) : RecyclerView.Adapter<FlashConfigAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val tvName: TextView = view.findViewById(R.id.tvAppName)
        val tvFilter: TextView = view.findViewById(R.id.tvFilter)
        val tvPattern: TextView = view.findViewById(R.id.tvPattern)
        val tvInterval: TextView = view.findViewById(R.id.tvInterval)
        val switchEnabled: SwitchCompat = view.findViewById(R.id.switchEnabled)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_flash_config, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val config = configs[position]
        holder.ivIcon.setImageDrawable(config.icon)
        holder.tvName.text = config.appName
        holder.tvFilter.text = if (config.filterText.isEmpty()) "Filter: [Any]" else "Filter: ${config.filterText}"
        holder.tvPattern.text = "Pattern: ${config.pattern}"
        holder.tvInterval.text = "Active: ${config.startTime} - ${config.endTime}"
        
        // Remove listener before setting state to avoid triggering onToggle
        holder.switchEnabled.setOnCheckedChangeListener(null)
        holder.switchEnabled.isChecked = config.isEnabled
        holder.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            onToggle(config, isChecked)
        }

        holder.itemView.setOnClickListener { onClick(config) }
    }

    override fun getItemCount() = configs.size

    fun updateData(newConfigs: List<FlashConfig>) {
        configs = newConfigs
        notifyDataSetChanged()
    }
}
