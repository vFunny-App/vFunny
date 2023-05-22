package vfunny.shortvideovfunnyapp.Lang.ui.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import vfunny.shortvideovfunnyapp.Post.model.Language

class PageViewModel() : ViewModel() {

    private val _index = MutableLiveData<Int>()

    fun setIndex(index: Int) {
        _index.value = index
    }

    val text: LiveData<String> = Transformations.map(_index) {
        "LANG : ${_index.value?.let { it1 -> Language.getAllLanguages()[it1].name }}"
    }
}