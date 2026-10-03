package com.example.data.model

data class SearchTotals(val count: Int = 0, val income: Long = 0, val expense: Long = 0)
data class MonthlyTotals(val monthString: String, val income: Long, val expense: Long, val count: Int)
