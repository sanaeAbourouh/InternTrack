package com.example.interntrack

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private lateinit var tvWelcomeTitle: TextView
    private lateinit var tvWelcomeSubtitle: TextView
    private lateinit var tvWelcomeDescription: TextView
    private lateinit var etSearch: EditText
    private lateinit var tvCountrySelector: TextView
    private lateinit var internshipsContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var sharedPref: SharedPreferences

    private var currentCategory = "Software Development"
    private var currentSearchQuery = ""

    // Country filter variables
    private var currentCountryCode = "all"
    private var currentCountryDisplay = "All"
    private var currentCountryFlag = "🌍"

    // Static cache that persists across fragment recreation
    companion object {
        private var cachedInternships = mutableListOf<InternshipItem>()
        private var lastCountryCode = ""
        private var lastKeyword = ""
        private var lastCategory = ""
        private var lastSearchQuery = ""
        private var hasLoadedOnce = false

        // Store last selected values to restore after recreation
        private var lastSelectedCountryCode = "all"
        private var lastSelectedCountryDisplay = "All"
        private var lastSelectedCountryFlag = "🌍"
        private var lastSelectedCategory = "Software Development"
        private var lastSelectedSearchQuery = ""
    }

    private var allInternships = mutableListOf<InternshipItem>()
    private var displayedCount = 0
    private val PAGE_SIZE = 3

    // Thread pool for parallel API calls
    private val executorService = Executors.newFixedThreadPool(5)

    private val TAG = "HomeFragment"

    // Country mapping with flags and API codes
    private data class CountryInfo(
        val displayName: String,
        val shortName: String,
        val flag: String,
        val countryCode: String
    )

    private val countries = listOf(
        CountryInfo("All Countries", "All", "🌍", "all"),
        CountryInfo("USA", "USA", "🇺🇸", "us"),
        CountryInfo("UK", "UK", "🇬🇧", "gb"),
        CountryInfo("Canada", "Canada", "🇨🇦", "ca"),
        CountryInfo("Australia", "Australia", "🇦🇺", "au"),
        CountryInfo("Germany", "Germany", "🇩🇪", "de"),
        CountryInfo("France", "France", "🇫🇷", "fr")
    )

    // List of countries to fetch when "All Countries" is selected
    private val allCountriesList = listOf("us", "gb", "ca", "au", "de", "fr")

    // My Adzuna API Credentials
    private val ADZUNA_APP_ID = "97c8f8d6"
    private val ADZUNA_API_KEY = "92dc63e9d52bcad5c05b63b375298634"

    data class InternshipItem(
        val id: String,
        val company: String,
        val title: String,
        val location: String,
        val description: String,
        val applyUrl: String
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Welcome Card Views
        tvWelcomeTitle = view.findViewById(R.id.tvWelcomeTitle)
        tvWelcomeSubtitle = view.findViewById(R.id.tvWelcomeSubtitle)
        tvWelcomeDescription = view.findViewById(R.id.tvWelcomeDescription)

        // Other Views
        etSearch = view.findViewById(R.id.etSearch)
        tvCountrySelector = view.findViewById(R.id.tvCountrySelector)
        internshipsContainer = view.findViewById(R.id.internshipsContainer)
        progressBar = view.findViewById(R.id.progressBar)

        // Category views
        val categorySoftware = view.findViewById<TextView>(R.id.categorySoftware)
        val categoryAI = view.findViewById<TextView>(R.id.categoryAI)
        val categoryCybersecurity = view.findViewById<TextView>(R.id.categoryCybersecurity)
        val categoryDataScience = view.findViewById<TextView>(R.id.categoryDataScience)
        val categoryMarketing = view.findViewById<TextView>(R.id.categoryMarketing)
        val categoryBusiness = view.findViewById<TextView>(R.id.categoryBusiness)
        val categoryFinance = view.findViewById<TextView>(R.id.categoryFinance)
        val categoryDesign = view.findViewById<TextView>(R.id.categoryDesign)

        // Get SharedPreferences
        sharedPref = requireActivity().getSharedPreferences("UserPrefs", AppCompatActivity.MODE_PRIVATE)

        // Get user name
        var userName = sharedPref.getString("userName", "")
        if (userName.isNullOrEmpty()) {
            userName = sharedPref.getString("user_name", "")
        }
        if (userName.isNullOrEmpty()) {
            userName = sharedPref.getString("name", "")
        }
        if (userName.isNullOrEmpty()) {
            userName = "User"
        }

        // Set Welcome Card Content
        tvWelcomeTitle.text = "👋 Welcome, $userName"
        tvWelcomeSubtitle.text = "Ready to discover new opportunities today?"
        tvWelcomeDescription.text = "Find internships, save favorites, and track your applications."

        // Restore last selected values
        restoreLastSelections()

        // Set up country selector button
        updateCountrySelectorDisplay()
        tvCountrySelector.setOnClickListener {
            showCountryFilterMenu()
        }

        // Set up category click listeners
        categorySoftware.setOnClickListener {
            updateCategory(categorySoftware, "Software Development")
            if (currentCategory != "Software Development") {
                currentCategory = "Software Development"
                saveCurrentState()
                loadInternshipsIfNeeded(true)
            }
        }
        categoryAI.setOnClickListener {
            updateCategory(categoryAI, "AI & Machine Learning")
            if (currentCategory != "AI & Machine Learning") {
                currentCategory = "AI & Machine Learning"
                saveCurrentState()
                loadInternshipsIfNeeded(true)
            }
        }
        categoryDataScience.setOnClickListener {
            updateCategory(categoryDataScience, "Data Science")
            if (currentCategory != "Data Science") {
                currentCategory = "Data Science"
                saveCurrentState()
                loadInternshipsIfNeeded(true)
            }
        }
        categoryCybersecurity.setOnClickListener {
            updateCategory(categoryCybersecurity, "Cybersecurity")
            if (currentCategory != "Cybersecurity") {
                currentCategory = "Cybersecurity"
                saveCurrentState()
                loadInternshipsIfNeeded(true)
            }
        }
        categoryMarketing.setOnClickListener {
            updateCategory(categoryMarketing, "Marketing")
            if (currentCategory != "Marketing") {
                currentCategory = "Marketing"
                saveCurrentState()
                loadInternshipsIfNeeded(true)
            }
        }
        categoryBusiness.setOnClickListener {
            updateCategory(categoryBusiness, "Business")
            if (currentCategory != "Business") {
                currentCategory = "Business"
                saveCurrentState()
                loadInternshipsIfNeeded(true)
            }
        }
        categoryFinance.setOnClickListener {
            updateCategory(categoryFinance, "Finance")
            if (currentCategory != "Finance") {
                currentCategory = "Finance"
                saveCurrentState()
                loadInternshipsIfNeeded(true)
            }
        }
        categoryDesign.setOnClickListener {
            updateCategory(categoryDesign, "Design")
            if (currentCategory != "Design") {
                currentCategory = "Design"
                saveCurrentState()
                loadInternshipsIfNeeded(true)
            }
        }

        // Search functionality
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearchQuery = s.toString().trim()
                saveCurrentState()
                val query = currentSearchQuery
                if (query.isNotEmpty()) {
                    loadInternshipsIfNeeded(true, query)
                } else {
                    loadInternshipsIfNeeded(true)
                }
            }
        })

        // Load internships - use cached data if available
        loadInternshipsIfNeeded(false)
    }

    private fun saveCurrentState() {
        lastSelectedCountryCode = currentCountryCode
        lastSelectedCountryDisplay = currentCountryDisplay
        lastSelectedCountryFlag = currentCountryFlag
        lastSelectedCategory = currentCategory
        lastSelectedSearchQuery = currentSearchQuery
    }

    private fun restoreLastSelections() {
        currentCountryCode = lastSelectedCountryCode
        currentCountryDisplay = lastSelectedCountryDisplay
        currentCountryFlag = lastSelectedCountryFlag
        currentCategory = lastSelectedCategory
        currentSearchQuery = lastSelectedSearchQuery
    }

    private fun loadInternshipsIfNeeded(forceReload: Boolean, customQuery: String = "") {
        val searchKeyword = if (customQuery.isNotEmpty()) {
            "$customQuery intern"
        } else if (currentCategory.isNotEmpty()) {
            "${currentCategory.lowercase()} intern"
        } else {
            "software engineering intern"
        }

        val isSameFilter = lastCountryCode == currentCountryCode &&
                lastKeyword == searchKeyword &&
                lastCategory == currentCategory &&
                lastSearchQuery == currentSearchQuery

        // If we have cached data and filters haven't changed, use cache
        if (!forceReload && hasLoadedOnce && isSameFilter && cachedInternships.isNotEmpty()) {
            Log.d(TAG, "🟢 USING CACHED INTERNSHIPS - No API call needed")
            Log.d(TAG, "   Country: $currentCountryCode, Category: $currentCategory, Search: '$currentSearchQuery'")
            allInternships.clear()
            allInternships.addAll(cachedInternships)
            displayedCount = 0
            internshipsContainer.removeAllViews()
            progressBar.visibility = View.GONE
            loadMoreInternships()
            return
        }

        // Otherwise, fetch new data
        Log.d(TAG, "🔄 FETCHING NEW INTERNSHIPS from API")
        Log.d(TAG, "   Country: $currentCountryCode, Category: $currentCategory, Search: '$currentSearchQuery'")

        lastCountryCode = currentCountryCode
        lastKeyword = searchKeyword
        lastCategory = currentCategory
        lastSearchQuery = currentSearchQuery

        showLoading(true)
        fetchAdzunaInternships(searchKeyword)
    }

    private fun showCountryFilterMenu() {
        val popupMenu = PopupMenu(requireContext(), tvCountrySelector)

        for (country in countries) {
            popupMenu.menu.add("${country.flag} ${country.displayName}")
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            val selectedText = menuItem.title.toString()
            val selectedCountry = countries.find { "${it.flag} ${it.displayName}" == selectedText }

            if (selectedCountry != null) {
                currentCountryCode = selectedCountry.countryCode
                currentCountryDisplay = selectedCountry.shortName
                currentCountryFlag = selectedCountry.flag
                updateCountrySelectorDisplay()
                saveCurrentState()
                Log.d(TAG, "🌍 Country changed to: $currentCountryDisplay ($currentCountryCode)")
                // Force reload when country changes
                loadInternshipsIfNeeded(true)
            }
            true
        }

        popupMenu.show()
    }

    private fun updateCountrySelectorDisplay() {
        tvCountrySelector.text = "$currentCountryFlag $currentCountryDisplay ▼"
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            progressBar.visibility = View.VISIBLE
            internshipsContainer.removeAllViews()
            displayedCount = 0
        } else {
            progressBar.visibility = View.GONE
        }
    }

    private fun updateCategory(selectedView: TextView, category: String) {
        currentCategory = category

        val categories = listOf(
            view?.findViewById<TextView>(R.id.categorySoftware),
            view?.findViewById<TextView>(R.id.categoryAI),
            view?.findViewById<TextView>(R.id.categoryCybersecurity),
            view?.findViewById<TextView>(R.id.categoryDataScience),
            view?.findViewById<TextView>(R.id.categoryMarketing),
            view?.findViewById<TextView>(R.id.categoryBusiness),
            view?.findViewById<TextView>(R.id.categoryFinance),
            view?.findViewById<TextView>(R.id.categoryDesign)
        )

        categories.forEach { cat ->
            cat?.setBackgroundResource(R.drawable.category_unselected)
            cat?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        }

        selectedView.setBackgroundResource(R.drawable.category_selected)
        selectedView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
    }

    private fun fetchAdzunaInternships(keyword: String) {
        if (currentCountryCode == "all") {
            fetchFromMultipleCountriesParallel(keyword)
        } else {
            fetchFromSingleCountry(keyword)
        }
    }

    private fun fetchFromSingleCountry(keyword: String) {
        Thread {
            try {
                val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
                val urlString = "https://api.adzuna.com/v1/api/jobs/$currentCountryCode/search/1?app_id=$ADZUNA_APP_ID&app_key=$ADZUNA_API_KEY&results_per_page=50&what=$encodedKeyword&content-type=application/json"

                Log.d(TAG, "📡 API Request: $urlString")

                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    val jsonObject = JSONObject(response.toString())
                    val resultsArray = jsonObject.getJSONArray("results")
                    val resultsList = mutableListOf<JSONObject>()
                    for (i in 0 until resultsArray.length()) {
                        resultsList.add(resultsArray.getJSONObject(i))
                    }

                    Handler(Looper.getMainLooper()).post {
                        if (!isAdded || activity == null) return@post
                        val internships = processInternshipResults(resultsList)
                        cachedInternships.clear()
                        cachedInternships.addAll(internships)
                        hasLoadedOnce = true
                        allInternships.clear()
                        allInternships.addAll(internships)
                        displayedCount = 0
                        internshipsContainer.removeAllViews()
                        progressBar.visibility = View.GONE
                        loadMoreInternships()
                        Log.d(TAG, "✅ API loaded ${internships.size} internships from single country")
                    }
                } else {
                    handleApiError(responseCode)
                }
                connection.disconnect()
            } catch (e: Exception) {
                handleException(e)
            }
        }.start()
    }

    private fun fetchFromMultipleCountriesParallel(keyword: String) {
        Log.d(TAG, "📡 Fetching from ${allCountriesList.size} countries in PARALLEL")
        val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
        val allResults = mutableListOf<JSONObject>()
        val lock = Any()
        var completedCount = 0
        val totalCountries = allCountriesList.size

        for (countryCode in allCountriesList) {
            executorService.submit {
                try {
                    val urlString = "https://api.adzuna.com/v1/api/jobs/$countryCode/search/1?app_id=$ADZUNA_APP_ID&app_key=$ADZUNA_API_KEY&results_per_page=30&what=$encodedKeyword&content-type=application/json"

                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0")

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        val response = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        reader.close()

                        val jsonObject = JSONObject(response.toString())
                        val resultsArray = jsonObject.getJSONArray("results")

                        synchronized(lock) {
                            for (i in 0 until resultsArray.length()) {
                                allResults.add(resultsArray.getJSONObject(i))
                            }
                            completedCount++
                        }
                    } else {
                        synchronized(lock) { completedCount++ }
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching from $countryCode: ${e.message}")
                    synchronized(lock) { completedCount++ }
                }

                if (completedCount == totalCountries) {
                    Handler(Looper.getMainLooper()).post {
                        if (!isAdded || activity == null) return@post
                        val internships = processInternshipResults(allResults)
                        cachedInternships.clear()
                        cachedInternships.addAll(internships)
                        hasLoadedOnce = true
                        allInternships.clear()
                        allInternships.addAll(internships)
                        displayedCount = 0
                        internshipsContainer.removeAllViews()
                        progressBar.visibility = View.GONE
                        loadMoreInternships()
                        Log.d(TAG, "✅ API loaded ${internships.size} internships from ${totalCountries} countries")
                    }
                }
            }
        }
    }

    private fun processInternshipResults(resultsArray: List<JSONObject>): List<InternshipItem> {
        val internships = mutableListOf<InternshipItem>()
        for (job in resultsArray) {
            val title = job.optString("title", "")

            if (!title.contains("intern", ignoreCase = true) &&
                !title.contains("internship", ignoreCase = true)) {
                continue
            }

            val companyObject = job.optJSONObject("company")
            val companyName = companyObject?.optString("display_name", "Unknown Company")
                ?: "Unknown Company"

            val locationObject = job.optJSONObject("location")
            val locationName = locationObject?.optString("display_name", "Remote")
                ?: "Remote"

            val internship = InternshipItem(
                id = job.optString("id", System.currentTimeMillis().toString()),
                company = companyName,
                title = title,
                location = locationName,
                description = job.optString("description", "No description available"),
                applyUrl = job.optString("redirect_url", "")
            )
            internships.add(internship)
        }
        Log.d(TAG, "📊 Filtered internships: ${internships.size}")
        return internships
    }

    private fun handleApiError(responseCode: Int) {
        Handler(Looper.getMainLooper()).post {
            if (!isAdded || activity == null) return@post
            progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "API Error $responseCode", Toast.LENGTH_LONG).show()
            showNoResultsMessage()
        }
    }

    private fun handleException(e: Exception) {
        Log.e(TAG, "Exception: ${e.message}", e)
        Handler(Looper.getMainLooper()).post {
            if (!isAdded || activity == null) return@post
            progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            showNoResultsMessage()
        }
    }

    private fun loadMoreInternships() {
        if (!isAdded) return

        if (allInternships.isEmpty()) {
            showNoResultsMessage()
            return
        }

        val nextBatch = allInternships.drop(displayedCount).take(PAGE_SIZE)

        if (nextBatch.isEmpty()) {
            val endText = TextView(requireContext())
            endText.text = "✨ You've seen all available internships"
            endText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            endText.setPadding(20, 24, 20, 24)
            endText.gravity = android.view.Gravity.CENTER
            internshipsContainer.addView(endText)
            return
        }

        for (internship in nextBatch) {
            addInternshipCard(internship)
        }

        displayedCount += nextBatch.size

        if (displayedCount < allInternships.size) {
            addSeeMoreButton()
        }
    }

    private fun addInternshipCard(internship: InternshipItem) {
        if (!isAdded) return

        val internshipView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_internship, internshipsContainer, false)

        val ivCompanyLogo = internshipView.findViewById<ImageView>(R.id.ivCompanyLogo)
        val tvCompanyName = internshipView.findViewById<TextView>(R.id.tvCompanyName)
        val tvInternshipTitle = internshipView.findViewById<TextView>(R.id.tvInternshipTitle)
        val tvLocation = internshipView.findViewById<TextView>(R.id.tvLocation)
        val tvDeadline = internshipView.findViewById<TextView>(R.id.tvDeadline)

        tvCompanyName.text = internship.company
        tvInternshipTitle.text = internship.title
        tvLocation.text = "📍 ${internship.location}"
        tvDeadline.text = internship.title

        ivCompanyLogo.setImageResource(R.drawable.company_placeholder)

        internshipView.setOnClickListener {
            val intent = Intent(requireContext(), InternshipDetailsActivity::class.java)
            intent.putExtra("INTERNSHIP_ID", internship.id)
            intent.putExtra("COMPANY", internship.company)
            intent.putExtra("TITLE", internship.title)
            intent.putExtra("LOCATION", internship.location)
            intent.putExtra("DESCRIPTION", internship.description)
            intent.putExtra("APPLY_URL", internship.applyUrl)
            startActivity(intent)
        }

        internshipsContainer.addView(internshipView)
    }

    private fun addSeeMoreButton() {
        if (!isAdded) return

        val seeMoreButton = TextView(requireContext())
        seeMoreButton.text = "🔍 See More Internships →"
        seeMoreButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark))
        seeMoreButton.setPadding(0, 24, 0, 24)
        seeMoreButton.gravity = android.view.Gravity.CENTER
        seeMoreButton.setOnClickListener {
            internshipsContainer.removeView(seeMoreButton)
            loadMoreInternships()
        }
        internshipsContainer.addView(seeMoreButton)
    }

    private fun showNoResultsMessage() {
        if (!isAdded) return

        internshipsContainer.removeAllViews()
        val emptyText = TextView(requireContext())
        emptyText.text = "No internships found.\nTry a different category or search term."
        emptyText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        emptyText.setPadding(20, 40, 20, 40)
        emptyText.gravity = android.view.Gravity.CENTER
        internshipsContainer.addView(emptyText)
    }

    override fun onDestroy() {
        super.onDestroy()
        executorService.shutdown()
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
        }
    }
}