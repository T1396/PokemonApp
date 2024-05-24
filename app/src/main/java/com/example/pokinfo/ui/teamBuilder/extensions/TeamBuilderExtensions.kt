package com.example.pokinfo.ui.teamBuilder.extensions

import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.pokinfo.R
import com.example.pokinfo.ui.teamBuilder.TeamBuilderFragment
import com.google.android.material.appbar.MaterialToolbar

fun TeamBuilderFragment.overrideNavigationLogic() {
    // override back-navigation (physical)
    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            showUnsavedChangesDialog()
        }
    }
    // override back pressed dispatcher and toolbar navigation to warn the user about losing data
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
    (activity as? AppCompatActivity)?.findViewById<MaterialToolbar>(R.id.mainToolbar)
        ?.setNavigationOnClickListener {
            showUnsavedChangesDialog()
        }
}