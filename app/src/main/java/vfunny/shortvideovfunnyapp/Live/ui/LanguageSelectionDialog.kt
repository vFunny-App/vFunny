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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.GenericTypeIndicator
import vfunny.shortvideovfunnyapp.LangUtils.LangManager
import vfunny.shortvideovfunnyapp.Login.model.User
import vfunny.shortvideovfunnyapp.Post.model.Language
import vfunny.shortvideovfunnyapp.R

class LanguageSelectionDialog(
    context: Context,
) : Dialog(context) {
    private lateinit var callback: BaseActivity.LanguageSelectionCallback
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

        User.currentKey()?.let { userId ->
            User.getLanguage(userId).get().addOnSuccessListener { dataSnapshot: DataSnapshot ->
                val allLanguage = Language.getAllLanguages().filter { it != Language.WORLDWIDE }
                // Get the selected languages from dataSnapshot
                val selectedLanguages =
                    (dataSnapshot.getValue(object : GenericTypeIndicator<List<Language>>() {})
                        ?.filter { it != Language.WORLDWIDE } ?: emptyList()).toMutableList()
                val languageMap = mutableMapOf<Language, Boolean>()
                for (language in allLanguage) {
                    languageMap[language] = selectedLanguages.any { it.name == language.name }
                }

                // Set up the RecyclerView
                recyclerView.layoutManager = LinearLayoutManager(context)
                recyclerView.adapter = LanguageAdapter(languageMap)
                // Set up the confirm button
                confirmButton.setOnClickListener {
                    // Get the selected languages from the adapter
                    val finalSelectedLanguages =
                        (recyclerView.adapter as LanguageAdapter).getSelectedLanguages()
                    // Save the selected languages to the database
                    User.currentKey()?.let { userId ->
                        LangManager.instance.setLanguage(userId, finalSelectedLanguages)
                    }
                    callback.onLanguageSelected()
                    // Dismiss the dialog
                    dismiss()
                }
                // Set up the cancelButton button
            }
        }
        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    fun setLanguageSelectionCallback(callback: BaseActivity.LanguageSelectionCallback) {
        this.callback = callback
    }
}


class LanguageAdapter(
    private val languagesMap: MutableMap<Language, Boolean>,
) : RecyclerView.Adapter<LanguageAdapter.ViewHolder>() {

    private val languagesList = languagesMap.entries.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = languagesList[position]
        val language = entry.key
        var languageSelected = entry.value
        holder.languageNameTextView.text = language.name
        holder.checkBox.isChecked = languageSelected
        holder.itemView.setOnClickListener {
            if (holder.checkBox.isChecked) {
                languagesMap[language] = false
                languageSelected = false
                holder.checkBox.isChecked = false
            } else {
                languagesMap[language] = true
                languageSelected = true
                holder.checkBox.isChecked = true
            }
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int {
        return languagesMap.size
    }

    fun getSelectedLanguages(): List<Language> {
        return languagesMap.filterValues { it }.keys.toList()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val languageNameTextView: TextView = itemView.findViewById(R.id.languageNameTextView)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
    }
}
