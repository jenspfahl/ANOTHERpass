package de.jepfa.yapm.viewmodel

import androidx.lifecycle.*
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.repository.CredentialRepository
import de.jepfa.yapm.ui.YapmApp
import kotlinx.coroutines.launch

class CredentialViewModel(private val repository: CredentialRepository) : ViewModel() {

    val allCredentials: LiveData<List<EncCredential>> = repository.getAll().asLiveData()

    fun getById(id: Int): LiveData<EncCredential> {
        return repository.getById(id).asLiveData()
    }

    fun findById(id: Int): LiveData<EncCredential?> {
        return repository.findById(id).asLiveData()
    }

    fun insert(credential: EncCredential) = viewModelScope.launch {
        credential.touchModify()
        repository.insert(credential)
    }    

    fun update(credential: EncCredential) = viewModelScope.launch {
        credential.touchModify()
        repository.update(credential)
    }

    fun delete(credential: EncCredential)  = viewModelScope.launch {
        credential.touchModify()
        repository.delete(credential)
    }
}


class CredentialViewModelFactory(private val app: YapmApp) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CredentialViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CredentialViewModel(app.credentialRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}