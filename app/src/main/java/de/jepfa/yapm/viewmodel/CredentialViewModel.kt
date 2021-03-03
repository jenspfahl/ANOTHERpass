package de.jepfa.yapm.viewmodel

import androidx.lifecycle.*
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.repository.CredentialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class CredentialViewModel(private val repository: CredentialRepository) : ViewModel() {

    val allCredentials: LiveData<List<EncCredential>> = repository.getAll().asLiveData()

    fun getById(id: Int): LiveData<EncCredential> {
        return repository.getById(id).asLiveData()
    }

    fun insert(credential: EncCredential) = viewModelScope.launch {
        repository.insert(credential)
    }    

    fun update(credential: EncCredential) = viewModelScope.launch {
        repository.update(credential)
    }

    fun delete(credential: EncCredential)  = viewModelScope.launch {
        repository.delete(credential)
    }
}


class CredentialViewModelFactory(private val repository: CredentialRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CredentialViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CredentialViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}