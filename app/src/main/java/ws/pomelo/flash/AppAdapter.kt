package ws.pomelo.flash

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(private val apps: List<AppInfo>, private val onClick: (AppInfo) -> Unit) :
    RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Now using your custom IDs from item_app.xml
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val name: TextView = view.findViewById(R.id.appName)
        val pkg: TextView = view.findViewById(R.id.appPackage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false) // Using your new layout
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.name.text = app.name
        holder.pkg.text = app.packageName
        holder.icon.setImageDrawable(app.icon)

        holder.itemView.setOnClickListener { onClick(app) }
    }

    override fun getItemCount() = apps.size
}