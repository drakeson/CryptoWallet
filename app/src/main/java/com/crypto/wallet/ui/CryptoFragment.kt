package com.crypto.wallet.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.crypto.wallet.R
import com.crypto.wallet.ui.viewmodel.CryptoViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.crypto.wallet.databinding.FragmentCryptoBinding
import com.crypto.wallet.ui.adapter.CoinAdapter


@AndroidEntryPoint
class CryptoFragment : Fragment(R.layout.fragment_crypto) {

    private var _binding: FragmentCryptoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CryptoViewModel by viewModels()
    private var adapter: CoinAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCryptoBinding.bind(view)

        adapter = CoinAdapter { coin ->
            // Handle coin click - navigate to detail fragment
            val fragment = CoinDetailFragment.newInstance(coin)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
        binding.coinList.layoutManager = LinearLayoutManager(requireContext())
        binding.coinList.adapter = adapter

        binding.progressBar.visibility = View.VISIBLE

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = viewModel.coins.value?.filter {
                    it.name.contains(newText ?: "", ignoreCase = true) ||
                            it.symbol.contains(newText ?: "", ignoreCase = true)
                }
                adapter?.submitList(filteredList)
                return true
            }
        })

        viewModel.coins.observe(viewLifecycleOwner) { list ->
            adapter?.submitList(list)
            binding.progressBar.visibility = View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(requireContext(), "Error: $message", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
        }

        viewModel.refreshData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.searchView.setOnQueryTextListener(null)
        binding.coinList.adapter = null
        adapter = null
        _binding = null
    }
}