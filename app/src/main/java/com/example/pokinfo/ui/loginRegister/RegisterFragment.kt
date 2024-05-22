package com.example.pokinfo.ui.loginRegister

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.pokinfo.BuildConfig
import com.example.pokinfo.viewModels.FirebaseViewModel
import com.example.pokinfo.R
import com.example.pokinfo.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment(), ContextProvider {

    private var _binding: FragmentRegisterBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val viewModel : FirebaseViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRegister.isEnabled = false

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val enteredUserName = !binding.tilUserName.editText?.text.isNullOrBlank()
                val enteredMail = !binding.tilEmail.editText?.text.isNullOrBlank()
                val enteredPasswords = !binding.tilPassword.editText?.text.isNullOrBlank() && !binding.tilConfirmPassword.editText?.text.isNullOrBlank()
                val passwordsEqual = binding.tilPassword.editText?.text.toString() == binding.tilConfirmPassword.editText?.text.toString()

                binding.btnRegister.isEnabled = enteredUserName && enteredMail && enteredPasswords && passwordsEqual
            }
        }
        binding.tilPassword.editText?.addTextChangedListener(textWatcher)
        binding.tilEmail.editText?.addTextChangedListener(textWatcher)
        binding.tilConfirmPassword.editText?.addTextChangedListener(textWatcher)

        binding.btnRegister.setOnClickListener {
            viewModel.register(
                binding.tilEmail.editText?.text.toString(),
                binding.tilPassword.editText?.text.toString(),
            )
        }

        binding.btnGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.nav_login)
        }

        binding.btnRegisterGoogle.setOnClickListener {
            viewModel.setUpGoogleSignIn(this, false)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getActivityContext(): Context {
        return requireActivity()
    }
}