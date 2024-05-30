package com.example.pokinfo.ui.loginRegister

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.pokinfo.R
import com.example.pokinfo.databinding.FragmentLoginBinding
import com.example.pokinfo.viewModels.AuthenticationViewModel


interface ContextProvider {
    fun getActivityContext(): Context
}

class LoginFragment : Fragment(), ContextProvider {

    private var _binding: FragmentLoginBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val viewModel: AuthenticationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailEditText = binding.tietMail
        val passwordEditText = binding.tietPassword
        fun validateInputs() {
            val isEmailValid = emailEditText.text.toString().trim().isNotEmpty()
            val isPasswordValid = passwordEditText.text.toString().trim().isNotEmpty()
            binding.btnLogin.isEnabled = isEmailValid && isPasswordValid
        }



        binding.btnLogin.setOnClickListener {
            viewModel.login(
                binding.tietMail.text.toString(),
                binding.tietPassword.text.toString()
            )
        }

        listOf(emailEditText, passwordEditText).forEach {
            it.addTextChangedListener {
                validateInputs()
            }
        }


        binding.btnGoogleLogin.setOnClickListener {
            viewModel.setUpGoogleSignIn(this, true)
        }

        binding.btnGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.nav_register)
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