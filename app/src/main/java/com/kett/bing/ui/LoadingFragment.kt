package com.kett.bing.ui

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.kett.bing.MainActivity
import com.kett.bing.R

class LoadingFragment : Fragment() {

    private lateinit var progressBar: ProgressBar
    private var progressAnimator: ValueAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.loading_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.progressBar)
        startProgressAnimation()
    }

    private fun startProgressAnimation() {
        progressAnimator = ValueAnimator.ofInt(0, 100).apply {
            duration = 4000
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Int
                progressBar.progress = progress

                if (progress == 100) {
                    view?.postDelayed({
                        if (isAdded && view != null) {  // <--- проверяем, что фрагмент прикреплен и view не null
                            navigateToMainScreen()
                        }
                    }, 300)
                }
            }
            start()
        }
    }

    private fun navigateToMainScreen() {
        if (!isAdded) return  // фрагмент уже отсоединён — прерываем

        (activity as? MainActivity)?.openMainFragment()
        parentFragmentManager.beginTransaction().remove(this).commit()
    }

    override fun onDestroyView() {
        progressAnimator?.cancel()
        progressAnimator = null
        super.onDestroyView()
    }
}
