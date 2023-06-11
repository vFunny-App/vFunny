package vfunny.shortvideovfunnyapp.Live.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import vfunny.shortvideovfunnyapp.Post.model.Language
import vfunny.shortvideovfunnyapp.R
import vfunny.shortvideovfunnyapp.models.LanguageViewEvent

internal class LanguageSelectionDialog(
    context: Context,
    private val click: (event: Any) -> Unit,
) : Dialog(context) {
    private lateinit var recyclerView: RecyclerView
    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button

    companion object {
        const val TAG = "LanguageSelectionDialog"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_language_selection)
        // Initialize views
        recyclerView = findViewById(R.id.recyclerView)
        confirmButton = findViewById(R.id.confirmButton)
        cancelButton = findViewById(R.id.cancelButton)
        // Set up the RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = LanguageAdapter()
        // Set up the confirm button
        confirmButton.setOnClickListener {
            // Get the selected languages from the adapter
            click(LanguageViewEvent.ConfirmSelection((recyclerView.adapter as LanguageAdapter).getLanguages()))
        }
        // Set up the cancelButton button
        cancelButton.setOnClickListener {
            click(LanguageViewEvent.CancelSelection)
            dismiss()
        }
        setCanceledOnTouchOutside(false)
        setCancelable(false)
    }

    fun showLanguageDialog(languagesMap: MutableMap<Language, Boolean>) {
        (recyclerView.adapter as LanguageAdapter).setLanguages(languagesMap)
        (recyclerView.adapter as LanguageAdapter).notifyDataSetChanged()
    }
}

class LanguageAdapter : RecyclerView.Adapter<LanguageAdapter.ViewHolder>() {
    private val languagesMap: MutableMap<Language, Boolean> = mutableMapOf()
    private val languagesList: List<Map.Entry<Language, Boolean>>
        get() = languagesMap.entries.toList()

    fun setLanguages(languagesMap: MutableMap<Language, Boolean>) {
        this.languagesMap.clear()
        this.languagesMap.putAll(languagesMap)
    }
    fun getLanguages(): MutableMap<Language, Boolean>{
        return languagesMap
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_language, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = languagesList[position]
        val language = entry.key
        val languageSelected = entry.value

        holder.languageNameTextView.text = language.name
        holder.checkBox.isChecked = languageSelected

        holder.itemView.setOnClickListener {
            toggleLanguage(language)
        }
    }

    override fun getItemCount(): Int = languagesMap.size

    private fun toggleLanguage(language: Language) {
        languagesMap[language] = !languagesMap[language]!!
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val languageNameTextView: TextView = itemView.findViewById(R.id.languageNameTextView)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
    }
}
