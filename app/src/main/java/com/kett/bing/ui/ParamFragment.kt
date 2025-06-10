package com.kett.bing.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.kett.bing.MainActivity
import com.kett.bing.R

class ParamFragment : Fragment() {

    private lateinit var ageInput: EditText
    private lateinit var weightInput: EditText
    private lateinit var levelGroup: RadioGroup
    private lateinit var calculateButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_param, container, false)

        ageInput = view.findViewById(R.id.ageInput)
        weightInput = view.findViewById(R.id.weightInput)
        levelGroup = view.findViewById(R.id.levelGroup)
        calculateButton = view.findViewById(R.id.calculateButton)

        val prefs = requireActivity().getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val savedAge = prefs.getInt("age", -1)
        val savedWeight = prefs.getFloat("weight", -1f)

        if (savedAge > 0) {
            ageInput.setText(savedAge.toString())
        }

        if (savedWeight > 0f) {
            weightInput.setText(savedWeight.toString())
        }

        // Восстановление уровня
        val savedLevel = prefs.getString("level", "Beginner") ?: "Beginner"
        when (savedLevel) {
            "Beginner" -> levelGroup.check(R.id.beginner)
            "Medium" -> levelGroup.check(R.id.medium)
            "Advanced" -> levelGroup.check(R.id.advanced)
        }

        // Слушатель изменения уровня
        levelGroup.setOnCheckedChangeListener { _, checkedId ->
            val level = when (checkedId) {
                R.id.beginner -> "Beginner"
                R.id.medium -> "Medium"
                R.id.advanced -> "Advanced"
                else -> "Beginner" // По умолчанию
            }

            // Сохранение выбранного уровня
            prefs.edit().apply {
                putString("level", level)
                apply()
            }
        }

        calculateButton.setOnClickListener {
            val age = ageInput.text.toString().toIntOrNull()
            val weight = weightInput.text.toString().toFloatOrNull()
            val levelId = levelGroup.checkedRadioButtonId
            val level = view.findViewById<RadioButton>(levelId).text.toString()

            if (age == null || weight == null) {
                Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show()
            } else if (age !in 10..100 || weight !in 30f..200f) {
                Toast.makeText(requireContext(), "Age must be 10–100, weight must be 30–200 kg", Toast.LENGTH_SHORT).show()
            } else {
                prefs.edit().apply {
                    putInt("age", age)
                    putFloat("weight", weight)
                    putString("level", level)
                    apply()
                }

                val planFragment = PlanFragment()
                planFragment.arguments = Bundle().apply {
                    putString("level", level)
                }

                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.mainFragmentContainer, planFragment)
                    .addToBackStack(null)
                    .commit()

                (requireActivity() as? MainActivity)?.showBottomNav()
                (requireActivity() as? MainActivity)?.updateNavIcons("plan")
            }
        }

        return view
    }
}
