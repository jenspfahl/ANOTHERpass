package de.jepfa.yapm.viewmodel

import android.content.Context
import androidx.lifecycle.*
import de.jepfa.yapm.database.repository.WebExtensionRepository
import de.jepfa.yapm.model.encrypted.EncWebExtension
import de.jepfa.yapm.ui.YapmApp
import kotlinx.coroutines.launch

class WebExtensionViewModel(private val repository: WebExtensionRepository) : ViewModel() {

    val allWebExtensions: LiveData<List<EncWebExtension>> = repository.getAll().asLiveData()

    fun getById(id: Int): LiveData<EncWebExtension> {
        return repository.getById(id).asLiveData()
    }

    fun insert(WebExtension: EncWebExtension, context: Context) = viewModelScope.launch {
        repository.insert(WebExtension)
    }

    fun update(WebExtension: EncWebExtension, context: Context) = viewModelScope.launch {
        repository.update(WebExtension)
    }

    fun delete(WebExtension: EncWebExtension)  = viewModelScope.launch {
        repository.delete(WebExtension)
    }
    fun deleteById(id: Int)  = viewModelScope.launch {
        repository.deleteById(id)
    }
}


class WebExtensionViewModelFactory(private val app: YapmApp) : ViewModelProvider.Factory {
     override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WebExtensionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WebExtensionViewModel(app.webExtensionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}