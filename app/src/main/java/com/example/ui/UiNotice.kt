package com.example.ui

data class UiNotice(val message: String, val actionLabel: String? = null, val onAction: (() -> Unit)? = null)
