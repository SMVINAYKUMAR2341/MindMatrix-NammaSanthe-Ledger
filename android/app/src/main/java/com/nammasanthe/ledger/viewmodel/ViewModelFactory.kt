package com.nammasanthe.ledger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nammasanthe.ledger.NammaSantheApp
import com.nammasanthe.ledger.security.DeviceIdHelper

class LedgerViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(LedgerViewModel::class.java) ->
            LedgerViewModel(NammaSantheApp.instance.repository) as T

        modelClass.isAssignableFrom(OcrViewModel::class.java) ->
            OcrViewModel(NammaSantheApp.instance) as T

        modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
            ProfileViewModel(NammaSantheApp.instance.profileStore) as T

        modelClass.isAssignableFrom(ConfirmationViewModel::class.java) -> {
            val app      = NammaSantheApp.instance
            val deviceId = DeviceIdHelper.getDeviceId(app)
            ConfirmationViewModel(
                confirmRepo    = app.confirmationRepo,
                ledgerRepo     = app.repository,
                vendorDeviceId = deviceId,
                appContext     = app.applicationContext
            ) as T
        }

        else -> throw IllegalArgumentException("Unknown ViewModel: $modelClass")
    }
}
