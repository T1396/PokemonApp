package com.example.pokinfo.ui.loginRegister

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.pokinfo.BuildConfig
import com.example.pokinfo.viewModels.FirebaseViewModel
import com.example.pokinfo.R
import com.example.pokinfo.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val viewModel: FirebaseViewModel by activityViewModels()
    private val webClientId = BuildConfig.webClientId


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

        binding.btnLogin.setOnClickListener {
            viewModel.login(
                binding.tietMail.text.toString(),
                binding.tietPassword.text.toString()
            )
        }

        binding.btnGoogleLogin.setOnClickListener {
            viewModel.startGoogleSignIn(webClientId)
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.nav_register)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}