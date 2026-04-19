package de.onemanprojects.klukka

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.divider.MaterialDivider

class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_about, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.about_version).text =
            getString(R.string.about_version_prefix, BuildConfig.VERSION_NAME)

        view.findViewById<View>(R.id.link_github_app).setOnClickListener {
            openUrl("https://github.com/placeholder/klukka-android")
        }
        view.findViewById<View>(R.id.link_github_backend).setOnClickListener {
            openUrl("https://github.com/placeholder/klukka-backend")
        }

        populateDependencies(view.findViewById(R.id.about_deps_container))

        view.findViewById<TextView>(R.id.about_license_text).text = MIT_LICENSE
    }

    private fun populateDependencies(container: LinearLayout) {
        val deps = listOf(
            "AndroidX Core KTX"             to "1.18.0",
            "AndroidX AppCompat"            to "1.7.1",
            "Material Components"           to "1.13.0",
            "AndroidX Activity"             to "1.13.0",
            "AndroidX ConstraintLayout"     to "2.2.1",
            "AndroidX Fragment KTX"         to "1.8.8",
            "AndroidX Lifecycle ViewModel"  to "2.9.0",
            "AndroidX RecyclerView"         to "1.4.0",
            "AndroidX SwipeRefreshLayout"   to "1.1.0",
            "AndroidX Security Crypto"      to "1.0.0",
            "Retrofit"                      to "2.11.0",
            "OkHttp"                        to "4.12.0",
            "Gson"                          to "2.11.0",
        )
        val inflater = LayoutInflater.from(requireContext())
        deps.forEachIndexed { index, (name, version) ->
            if (index > 0) {
                container.addView(MaterialDivider(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                })
            }
            val row = inflater.inflate(R.layout.item_dep_row, container, false)
            row.findViewById<TextView>(R.id.dep_name).text = name
            row.findViewById<TextView>(R.id.dep_version).text = version
            container.addView(row)
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    companion object {
        private val MIT_LICENSE = """
MIT License

Copyright (c) 2024–2025 1ManProjects

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
        """.trimIndent()
    }
}
