package com.kett.bing.ui

import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.kett.bing.R
//import com.kett.bing.databinding.FragmentCalendarBinding

class CalendarFragment : Fragment() {

//    private var _binding: FragmentCalendarBinding? = null
//    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        _binding = FragmentCalendarBinding.inflate(inflater, container, false)

//        return binding.root
        return null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Кнопка назад
        val backButton: ImageView = view.findViewById(R.id.backButton)
        backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        _binding = null
    }
}
