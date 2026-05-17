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
import androidx.fragment.app.viewModels
import com.google.android.material.divider.MaterialDivider

class AboutFragment : Fragment() {

    private val viewModel: AboutViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_about, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.about_version).text =
            getString(R.string.about_version_prefix, BuildConfig.VERSION_NAME)

        val tvBackendVersion = view.findViewById<TextView>(R.id.about_backend_version)
        viewModel.backendVersion.observe(viewLifecycleOwner) { version ->
            if (version != null) {
                tvBackendVersion.text = getString(R.string.about_backend_version_prefix, version)
                tvBackendVersion.visibility = View.VISIBLE
            } else {
                tvBackendVersion.visibility = View.GONE
            }
        }
        viewModel.loadServerInfo()

        view.findViewById<View>(R.id.link_github_app).setOnClickListener {
            openUrl("https://github.com/1manprojects/klukka-android")
        }
        view.findViewById<View>(R.id.link_github_backend).setOnClickListener {
            openUrl("https://github.com/1manprojects/klukka")
        }

        populateDependencies(view.findViewById(R.id.about_deps_container))

        view.findViewById<TextView>(R.id.about_license_text).text = MIT_LICENSE
    }

    private fun populateDependencies(container: LinearLayout) {
        data class Dep(val name: String, val license: String, val version: String)
        val deps = listOf(
            Dep("AndroidX Core KTX",            "Apache-2.0", "1.18.0"),
            Dep("AndroidX AppCompat",            "Apache-2.0", "1.7.1"),
            Dep("Material Components",           "Apache-2.0", "1.13.0"),
            Dep("AndroidX Activity",             "Apache-2.0", "1.13.0"),
            Dep("AndroidX ConstraintLayout",     "Apache-2.0", "2.2.1"),
            Dep("AndroidX Fragment KTX",         "Apache-2.0", "1.8.8"),
            Dep("AndroidX Lifecycle ViewModel",  "Apache-2.0", "2.9.0"),
            Dep("AndroidX RecyclerView",         "Apache-2.0", "1.4.0"),
            Dep("AndroidX SwipeRefreshLayout",   "Apache-2.0", "1.1.0"),
            Dep("AndroidX Security Crypto",      "Apache-2.0", "1.0.0"),
            Dep("Retrofit",                      "Apache-2.0", "2.11.0"),
            Dep("OkHttp",                        "Apache-2.0", "4.12.0"),
            Dep("Gson",                          "Apache-2.0", "2.11.0"),
        )
        val inflater = LayoutInflater.from(requireContext())
        deps.forEachIndexed { index, dep ->
            if (index > 0) {
                container.addView(MaterialDivider(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                })
            }
            val row = inflater.inflate(R.layout.item_dep_row, container, false)
            row.findViewById<TextView>(R.id.dep_name).text = dep.name
            row.findViewById<TextView>(R.id.dep_version).text = "${dep.license} · ${dep.version}"
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
