package eim.project.mobile_banking_android_app.payments

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import eim.project.mobile_banking_android_app.databinding.FragmentPaymentsBinding

class PaymentsFragment : Fragment() {

    private var _binding: FragmentPaymentsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val cardNumberSpinner: Spinner = binding.cardNumberSpinner
        val cardNumbersList = listOf("1234 5678 9012 3456", "9876 5432 1098 7654")
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, cardNumbersList)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        cardNumberSpinner.adapter = adapter
        cardNumberSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Get the selected card number
                val selectedCardNumber = parent.getItemAtPosition(position) as String
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}