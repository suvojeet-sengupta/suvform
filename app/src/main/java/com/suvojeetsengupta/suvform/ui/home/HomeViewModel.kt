package com.suvojeetsengupta.suvform.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeetsengupta.suvform.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    fun signOut(context: Context, onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut(context)
            onDone()
        }
    }
}
