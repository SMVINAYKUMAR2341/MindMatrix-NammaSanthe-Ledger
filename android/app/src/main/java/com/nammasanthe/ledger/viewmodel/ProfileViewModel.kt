package com.nammasanthe.ledger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nammasanthe.ledger.NammaSantheApp
import com.nammasanthe.ledger.data.repo.AppProfile
import com.nammasanthe.ledger.data.repo.ProfileStore
import com.nammasanthe.ledger.util.PinHasher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(private val store: ProfileStore) : ViewModel() {
    val profile = store.profile.stateIn(viewModelScope, SharingStarted.Eagerly, AppProfile())

    fun save(profile: AppProfile) = viewModelScope.launch { store.update(profile) }

    fun setPin(pin: String?) = viewModelScope.launch {
        store.setPin(pin?.let { PinHasher.hash(it) })
    }

    companion object {
        fun create(): ProfileViewModel = ProfileViewModel(NammaSantheApp.instance.profileStore)
    }
}
