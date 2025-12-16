package com.caloracker.ui.history

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.caloracker.databinding.FragmentHistoryBinding
import com.caloracker.ui.adapters.FoodLogAdapter
import com.caloracker.util.DateUtils
import com.caloracker.util.gone
import com.caloracker.util.visible
import java.util.*

/**
 * Fragment displaying historical food logs.
 */
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private val adapter = FoodLogAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeData()
    }

    /**
     * Set up RecyclerView for history logs.
     */
    private fun setupRecyclerView() {
        binding.rvHistoryLogs.adapter = adapter
    }

    /**
     * Set up click listeners.
     */
    private fun setupClickListeners() {
        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }
    }

    /**
     * Observe ViewModel data.
     */
    private fun observeData() {
        // Observe selected date
        viewModel.selectedDate.observe(viewLifecycleOwner) { timestamp ->
            binding.tvSelectedDate.text = DateUtils.formatDate(timestamp)
        }

        // Observe food logs
        viewModel.foodLogs.observe(viewLifecycleOwner) { logs ->
            adapter.submitList(logs)

            // Show/hide empty state
            if (logs.isEmpty()) {
                binding.tvEmptyState.visible()
                binding.rvHistoryLogs.gone()
                binding.cardSummary.gone()
            } else {
                binding.tvEmptyState.gone()
                binding.rvHistoryLogs.visible()
                binding.cardSummary.visible()
            }
        }

        // Observe total calories
        viewModel.totalCalories.observe(viewLifecycleOwner) { calories ->
            binding.tvSummaryCalories.text = viewModel.formatCaloriesSummary(calories)
        }

        // Observe total macros
        viewModel.totalMacros.observe(viewLifecycleOwner) { macros ->
            binding.tvSummaryMacros.text = viewModel.formatMacrosSummary(macros)
        }
    }

    /**
     * Show date picker dialog.
     */
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val currentDate = viewModel.selectedDate.value ?: System.currentTimeMillis()
        calendar.timeInMillis = currentDate

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                // Create selected date timestamp
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth, 0, 0, 0)
                selectedCalendar.set(Calendar.MILLISECOND, 0)

                viewModel.setSelectedDate(selectedCalendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
