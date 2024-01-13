package de.jepfa.yapm.viewmodel

import android.content.Context
import androidx.lifecycle.*
import de.jepfa.yapm.database.repository.UsernameTemplateRepository
import de.jepfa.yapm.model.encrypted.EncUsernameTemplate
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.ui.YapmApp
import kotlinx.coroutines.launch

class UsernameTemplateViewModel(private val repository: UsernameTemplateRepository) : ViewModel() {

    val allUsernameTemplates: LiveData<List<EncUsernameTemplate>> = repository.getAll().asLiveData()

    fun getById(id: Int): LiveData<EncUsernameTemplate> {
        return repository.getById(id).asLiveData()
    }

    fun insert(usernameTemplate: EncUsernameTemplate, context: Context) = viewModelScope.launch {
        repository.insert(usernameTemplate)
        PreferenceService.putCurrentDate(PreferenceService.DATA_VAULT_MODIFIED_AT, context)
    }

    fun update(usernameTemplate: EncUsernameTemplate, context: Context) = viewModelScope.launch {
        repository.update(usernameTemplate)
        PreferenceService.putCurrentDate(PreferenceService.DATA_VAULT_MODIFIED_AT, context)
    }

    fun delete(usernameTemplate: EncUsernameTemplate)  = viewModelScope.launch {
        repository.delete(usernameTemplate)
    }
    fun deleteById(id: Int)  = viewModelScope.launch {
        repository.deleteById(id)
    }
}


class UsernameTemplateViewModelFactory(private val app: YapmApp) : ViewModelProvider.Factory {
     override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UsernameTemplateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UsernameTemplateViewModel(app.usernameTemplateRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}