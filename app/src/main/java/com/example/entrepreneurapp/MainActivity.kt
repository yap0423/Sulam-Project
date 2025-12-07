package com.example.entrepreneurapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.entrepreneurapp.databinding.ActivityMainBinding
import com.example.entrepreneurapp.repository.AuthRepository
import com.example.entrepreneurapp.ui.auth.LoginActivity
import com.example.entrepreneurapp.ui.profile.EditProfileActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.entrepreneurapp.repository.FarmRepository
import com.example.entrepreneurapp.ui.farm.AddFarmActivity
import android.widget.LinearLayout
import com.example.entrepreneurapp.models.Farm
import com.example.entrepreneurapp.ui.farm.EditFarmActivity
import com.example.entrepreneurapp.models.Business
import com.example.entrepreneurapp.repository.BusinessRepository
import com.example.entrepreneurapp.ui.business.AddBusinessActivity
import com.example.entrepreneurapp.ui.business.EditBusinessActivity
import android.graphics.Color
import android.widget.ProgressBar
import com.example.entrepreneurapp.models.Certification
import com.example.entrepreneurapp.repository.CertificationRepository
import com.example.entrepreneurapp.ui.certification.AddCertificationActivity
import com.example.entrepreneurapp.ui.certification.EditCertificationActivity
import java.text.SimpleDateFormat
import java.util.*
import com.example.entrepreneurapp.ui.profile.ChangePasswordActivity
import com.example.entrepreneurapp.models.Announcement
import com.example.entrepreneurapp.repository.AnnouncementRepository
import com.example.entrepreneurapp.ui.announcements.*
import com.google.android.material.chip.Chip
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.entrepreneurapp.ui.planner.ProductionPlannerActivity
import android.widget.TextView
import android.widget.EditText
import android.widget.ImageButton
import com.google.android.material.card.MaterialCardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.entrepreneurapp.models.SearchResult
import com.example.entrepreneurapp.repository.SearchRepository
import com.example.entrepreneurapp.ui.home.SearchAnnouncementsAdapter
import com.example.entrepreneurapp.ui.home.SearchBusinessesAdapter
import com.example.entrepreneurapp.ui.home.SearchFarmsAdapter
import com.example.entrepreneurapp.ui.home.SearchPeopleAdapter


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authRepository = AuthRepository()
    private val firestore = FirebaseFirestore.getInstance()

    private var currentScreen = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is logged in
        if (!authRepository.isUserLoggedIn()) {
            navigateToLogin()
            return
        }

        loadUserInfo()

        // Handle navigation from other screens
        when {
            intent.getBooleanExtra("show_home", false) -> {
                binding.bottomNavigation.selectedItemId = R.id.nav_home
                showHomeScreen()
            }
            intent.getBooleanExtra("show_announcements", false) -> {
                binding.bottomNavigation.selectedItemId = R.id.nav_announcements
                showAnnouncementsScreen()
            }
            intent.getBooleanExtra("show_profile", false) -> {
                binding.bottomNavigation.selectedItemId = R.id.nav_profile
                showProfileScreen()
            }
            else -> {
                // Default to home
                showHomeScreen()
            }
        }

        setupBottomNavigation()

        // Load default screen (Home)
        showHomeScreen()
    }

    override fun onResume() {
        super.onResume()

        // Reload current screen to reflect any changes
        when (binding.bottomNavigation.selectedItemId) {
            R.id.nav_profile -> showProfileScreen()
            R.id.nav_announcements -> showAnnouncementsScreen()
            // Don't reload home as it's just a placeholder
        }
    }

    private fun loadUserInfo() {
        val currentUser = authRepository.getCurrentUser() ?: return

        lifecycleScope.launch {
            try {
                val documentSnapshot = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()

                val userName = documentSnapshot.getString("name") ?: "User"
                binding.tvUserBusiness.text = "Welcome, $userName"

            } catch (e: Exception) {
                binding.tvUserBusiness.text = "Welcome"
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showHomeScreen()
                    true
                }
                R.id.nav_profile -> {
                    showProfileScreen()
                    true
                }
                R.id.nav_announcements -> {
                    showAnnouncementsScreen()
                    true
                }
                R.id.nav_planner -> {
                    val intent = Intent(this, ProductionPlannerActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }


    private fun showHomeScreen() {
        currentScreen = "home"
        binding.tvAppTitle.text = "PineAForm"

        // Inflate new Home layout into fragment container
        binding.fragmentContainer.removeAllViews()
        val homeView = layoutInflater.inflate(R.layout.activity_home, binding.fragmentContainer, false)
        binding.fragmentContainer.addView(homeView)

        // Setup Home screen content
        setupNewHomeScreen(homeView)
    }

    private fun setupNewHomeScreen(view: View) {
        val currentUser = authRepository.getCurrentUser() ?: return

        // Find views
        val searchBarContainer = view.findViewById<LinearLayout>(R.id.searchBarContainer)
        val searchResultsOverlay = view.findViewById<LinearLayout>(R.id.searchResultsOverlay)
        val etSearchQuery = view.findViewById<EditText>(R.id.etSearchQuery)
        val btnCloseSearch = view.findViewById<ImageButton>(R.id.btnCloseSearch)
        val searchProgressBar = view.findViewById<ProgressBar>(R.id.searchProgressBar)
        val tvSearchResultsCount = view.findViewById<TextView>(R.id.tvSearchResultsCount)
        val mainHomeContent = view.findViewById<androidx.core.widget.NestedScrollView>(R.id.mainHomeContent)

        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        val tvMyGroup = view.findViewById<TextView>(R.id.tvMyGroup)
        val tvGroupMembersCount = view.findViewById<TextView>(R.id.tvGroupMembersCount)
        val tvGroupAvatars = view.findViewById<TextView>(R.id.tvGroupAvatars)
        val cardMyGroup = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardMyGroup)
        val cardPostUpdate = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardPostUpdate)
        val cardScheduleHarvest = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardScheduleHarvest)
        val tvSeeAllAnnouncements = view.findViewById<TextView>(R.id.tvSeeAllAnnouncements)
        val rvCommunityFeed = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvCommunityFeed)
        val progressBarFeed = view.findViewById<ProgressBar>(R.id.progressBarFeed)
        val tvEmptyFeed = view.findViewById<TextView>(R.id.tvEmptyFeed)

        // Search views
        val rvPeople = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvPeople)
        val rvFarms = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFarms)
        val rvBusinesses = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvBusinesses)
        val rvAnnouncements = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvAnnouncements)

        val tvPeopleHeader = view.findViewById<TextView>(R.id.tvPeopleHeader)
        val tvFarmsHeader = view.findViewById<TextView>(R.id.tvFarmsHeader)
        val tvBusinessesHeader = view.findViewById<TextView>(R.id.tvBusinessesHeader)
        val tvAnnouncementsHeader = view.findViewById<TextView>(R.id.tvAnnouncementsHeader)
        val tvNoResults = view.findViewById<TextView>(R.id.tvNoResults)

        val peopleDivider = view.findViewById<View>(R.id.peopleDivider)
        val farmsDivider = view.findViewById<View>(R.id.farmsDivider)
        val businessesDivider = view.findViewById<View>(R.id.businessesDivider)
        val announcementsDivider = view.findViewById<View>(R.id.announcementsDivider)

        // Setup search adapters
        val searchPeopleAdapter = SearchPeopleAdapter { person ->
            showPersonProfile(person.userId)
        }

        val searchFarmsAdapter = SearchFarmsAdapter { farm ->
            showPersonProfile(farm.ownerId)
        }

        val searchBusinessesAdapter = SearchBusinessesAdapter { business ->
            showPersonProfile(business.ownerId)
        }

        val searchAnnouncementsAdapter = SearchAnnouncementsAdapter { announcementResult ->
            val intent = Intent(this, AnnouncementDetailsActivity::class.java)
            intent.putExtra("ANNOUNCEMENT_ID", announcementResult.announcement.id)
            startActivity(intent)
        }

        rvPeople.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvPeople.adapter = searchPeopleAdapter

        rvFarms.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvFarms.adapter = searchFarmsAdapter

        rvBusinesses.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvBusinesses.adapter = searchBusinessesAdapter

        rvAnnouncements.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvAnnouncements.adapter = searchAnnouncementsAdapter

        // Load user data
        lifecycleScope.launch {
            try {
                val documentSnapshot = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()

                val userName = documentSnapshot.getString("name") ?: "User"
                val region = documentSnapshot.getString("region") ?: "Unknown"

                tvWelcome?.text = "Welcome back, $userName!"
                tvMyGroup?.text = "👥 Group: $region"

                // Load group member count
                loadGroupMemberCount(region, tvGroupMembersCount, tvGroupAvatars)

            } catch (e: Exception) {
                tvWelcome?.text = "Welcome back!"
            }
        }

        // Search bar click
        searchBarContainer?.setOnClickListener {
            searchResultsOverlay?.visibility = View.VISIBLE
            mainHomeContent?.visibility = View.GONE
            etSearchQuery?.requestFocus()

            // Show keyboard
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(etSearchQuery, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }

        // Close search
        btnCloseSearch?.setOnClickListener {
            searchResultsOverlay?.visibility = View.GONE
            mainHomeContent?.visibility = View.VISIBLE
            etSearchQuery?.text?.clear()

            // Hide keyboard
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(etSearchQuery?.windowToken, 0)
        }

        // Search query listener
        etSearchQuery?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString().trim()
                if (query.length >= 2) {
                    performSearch(
                        query,
                        searchProgressBar,
                        tvSearchResultsCount,
                        searchPeopleAdapter,
                        searchFarmsAdapter,
                        searchBusinessesAdapter,
                        searchAnnouncementsAdapter,
                        tvPeopleHeader,
                        tvFarmsHeader,
                        tvBusinessesHeader,
                        tvAnnouncementsHeader,
                        rvPeople,
                        rvFarms,
                        rvBusinesses,
                        rvAnnouncements,
                        peopleDivider,
                        farmsDivider,
                        businessesDivider,
                        announcementsDivider,
                        tvNoResults
                    )
                }
            }
        })

        // Quick actions
        cardPostUpdate?.setOnClickListener {
            val intent = Intent(this, CreateAnnouncementActivity::class.java)
            startActivity(intent)
        }

        cardScheduleHarvest?.setOnClickListener {
            val intent = Intent(this, ProductionPlannerActivity::class.java)
            startActivity(intent)
        }

        // My Group card
        cardMyGroup?.setOnClickListener {
            val intent = Intent(this, ProductionPlannerActivity::class.java)
            startActivity(intent)
        }

        // See all announcements
        tvSeeAllAnnouncements?.setOnClickListener {
            binding.bottomNavigation.selectedItemId = R.id.nav_announcements
        }

        // Load community feed (limited to 5 recent)
        loadCommunityFeed(rvCommunityFeed, progressBarFeed, tvEmptyFeed)
    }

    private fun loadGroupMemberCount(
        region: String,
        tvGroupMembersCount: TextView?,
        tvGroupAvatars: TextView?
    ) {
        lifecycleScope.launch {
            try {
                val usersSnapshot = firestore.collection("users")
                    .whereEqualTo("region", region)
                    .get()
                    .await()

                val count = usersSnapshot.documents.size
                tvGroupMembersCount?.text = "👥  $count"

                // Get avatars
                val avatars = usersSnapshot.documents.mapNotNull { doc ->
                    doc.getString("avatar")
                }.take(4).joinToString("")

                tvGroupAvatars?.text = avatars.ifEmpty { "👤" }

            } catch (e: Exception) {
                tvGroupMembersCount?.text = "👥  1"
            }
        }
    }

    private fun performSearch(
        query: String,
        progressBar: ProgressBar?,
        tvResultsCount: TextView?,
        peopleAdapter: SearchPeopleAdapter,
        farmsAdapter: SearchFarmsAdapter,
        businessesAdapter: SearchBusinessesAdapter,
        announcementsAdapter: SearchAnnouncementsAdapter,
        tvPeopleHeader: TextView?,
        tvFarmsHeader: TextView?,
        tvBusinessesHeader: TextView?,
        tvAnnouncementsHeader: TextView?,
        rvPeople: androidx.recyclerview.widget.RecyclerView?,
        rvFarms: androidx.recyclerview.widget.RecyclerView?,
        rvBusinesses: androidx.recyclerview.widget.RecyclerView?,
        rvAnnouncements: androidx.recyclerview.widget.RecyclerView?,
        peopleDivider: View?,
        farmsDivider: View?,
        businessesDivider: View?,
        announcementsDivider: View?,
        tvNoResults: TextView?
    ) {
        progressBar?.visibility = View.VISIBLE
        val searchRepository = SearchRepository()

        lifecycleScope.launch {
            val result = searchRepository.search(query)

            progressBar?.visibility = View.GONE

            result.onSuccess { results ->
                val totalCount = results.getTotalCount()
                tvResultsCount?.text = "Search Results ($totalCount found)"

                if (results.isEmpty()) {
                    tvNoResults?.visibility = View.VISIBLE
                    hideAllSearchSections(
                        tvPeopleHeader, tvFarmsHeader, tvBusinessesHeader, tvAnnouncementsHeader,
                        rvPeople, rvFarms, rvBusinesses, rvAnnouncements,
                        peopleDivider, farmsDivider, businessesDivider, announcementsDivider
                    )
                } else {
                    tvNoResults?.visibility = View.GONE

                    // People
                    if (results.people.isNotEmpty()) {
                        tvPeopleHeader?.visibility = View.VISIBLE
                        tvPeopleHeader?.text = "👥 People (${results.people.size})"
                        peopleDivider?.visibility = View.VISIBLE
                        rvPeople?.visibility = View.VISIBLE
                        peopleAdapter.submitList(results.people)
                    } else {
                        tvPeopleHeader?.visibility = View.GONE
                        peopleDivider?.visibility = View.GONE
                        rvPeople?.visibility = View.GONE
                    }

                    // Farms
                    if (results.farms.isNotEmpty()) {
                        tvFarmsHeader?.visibility = View.VISIBLE
                        tvFarmsHeader?.text = "🌾 Farms (${results.farms.size})"
                        farmsDivider?.visibility = View.VISIBLE
                        rvFarms?.visibility = View.VISIBLE
                        farmsAdapter.submitList(results.farms)
                    } else {
                        tvFarmsHeader?.visibility = View.GONE
                        farmsDivider?.visibility = View.GONE
                        rvFarms?.visibility = View.GONE
                    }

                    // Businesses
                    if (results.businesses.isNotEmpty()) {
                        tvBusinessesHeader?.visibility = View.VISIBLE
                        tvBusinessesHeader?.text = "🏢 Businesses (${results.businesses.size})"
                        businessesDivider?.visibility = View.VISIBLE
                        rvBusinesses?.visibility = View.VISIBLE
                        businessesAdapter.submitList(results.businesses)
                    } else {
                        tvBusinessesHeader?.visibility = View.GONE
                        businessesDivider?.visibility = View.GONE
                        rvBusinesses?.visibility = View.GONE
                    }

                    // Announcements
                    if (results.announcements.isNotEmpty()) {
                        tvAnnouncementsHeader?.visibility = View.VISIBLE
                        tvAnnouncementsHeader?.text = "💰 Announcements (${results.announcements.size})"
                        announcementsDivider?.visibility = View.VISIBLE
                        rvAnnouncements?.visibility = View.VISIBLE
                        announcementsAdapter.submitList(results.announcements)
                    } else {
                        tvAnnouncementsHeader?.visibility = View.GONE
                        announcementsDivider?.visibility = View.GONE
                        rvAnnouncements?.visibility = View.GONE
                    }
                }
            }.onFailure { exception ->
                Toast.makeText(this@MainActivity, "Search error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hideAllSearchSections(
        tvPeopleHeader: TextView?,
        tvFarmsHeader: TextView?,
        tvBusinessesHeader: TextView?,
        tvAnnouncementsHeader: TextView?,
        rvPeople: androidx.recyclerview.widget.RecyclerView?,
        rvFarms: androidx.recyclerview.widget.RecyclerView?,
        rvBusinesses: androidx.recyclerview.widget.RecyclerView?,
        rvAnnouncements: androidx.recyclerview.widget.RecyclerView?,
        peopleDivider: View?,
        farmsDivider: View?,
        businessesDivider: View?,
        announcementsDivider: View?
    ) {
        tvPeopleHeader?.visibility = View.GONE
        tvFarmsHeader?.visibility = View.GONE
        tvBusinessesHeader?.visibility = View.GONE
        tvAnnouncementsHeader?.visibility = View.GONE

        rvPeople?.visibility = View.GONE
        rvFarms?.visibility = View.GONE
        rvBusinesses?.visibility = View.GONE
        rvAnnouncements?.visibility = View.GONE

        peopleDivider?.visibility = View.GONE
        farmsDivider?.visibility = View.GONE
        businessesDivider?.visibility = View.GONE
        announcementsDivider?.visibility = View.GONE
    }

    private fun loadCommunityFeed(
        recyclerView: androidx.recyclerview.widget.RecyclerView?,
        progressBar: ProgressBar?,
        tvEmpty: TextView?
    ) {
        progressBar?.visibility = View.VISIBLE
        val announcementRepository = AnnouncementRepository()
        val currentUserId = authRepository.getCurrentUser()?.uid ?: ""

        lifecycleScope.launch {
            val result = announcementRepository.getAllAnnouncements()

            progressBar?.visibility = View.GONE

            result.onSuccess { announcements ->
                if (announcements.isEmpty()) {
                    tvEmpty?.visibility = View.VISIBLE
                    recyclerView?.visibility = View.GONE
                } else {
                    tvEmpty?.visibility = View.GONE
                    recyclerView?.visibility = View.VISIBLE

                    // Take only first 5 announcements
                    val limitedAnnouncements = announcements.take(5)

                    val adapter = AnnouncementsAdapter(
                        announcements = limitedAnnouncements,
                        currentUserId = currentUserId,
                        onAnnouncementClick = { announcement ->
                            val intent = Intent(this@MainActivity, AnnouncementDetailsActivity::class.java)
                            intent.putExtra("ANNOUNCEMENT_ID", announcement.id)
                            startActivity(intent)
                        },
                        onLikeClick = { announcement ->
                            likeAnnouncementHome(announcement, recyclerView)
                        },
                        onCommentClick = { announcement ->
                            val intent = Intent(this@MainActivity, AnnouncementDetailsActivity::class.java)
                            intent.putExtra("ANNOUNCEMENT_ID", announcement.id)
                            startActivity(intent)
                        },
                        onMoreClick = { announcement ->
                            // Handle more options if needed
                        }
                    )

                    recyclerView?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@MainActivity)
                    recyclerView?.adapter = adapter
                }
            }.onFailure { exception ->
                Toast.makeText(this@MainActivity, "Error loading feed: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun likeAnnouncementHome(announcement: Announcement, recyclerView: androidx.recyclerview.widget.RecyclerView?) {
        val announcementRepository = AnnouncementRepository()

        lifecycleScope.launch {
            val result = announcementRepository.likeAnnouncement(announcement.id)

            result.onSuccess {
                // Reload feed
                val progressBar: ProgressBar? = null
                val tvEmpty: TextView? = null
                loadCommunityFeed(recyclerView, progressBar, tvEmpty)
            }.onFailure { exception ->
                Toast.makeText(this@MainActivity, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPersonProfile(userId: String) { val searchRepository = SearchRepository()

        val dialogView = layoutInflater.inflate(R.layout.activity_view_profile, null, false)

        // Find views
        val tvAvatar = dialogView.findViewById<TextView>(R.id.tvAvatar)
        val tvUserName = dialogView.findViewById<TextView>(R.id.tvUserName)
        val tvUserEmail = dialogView.findViewById<TextView>(R.id.tvUserEmail)
        val tvUserEmailDetail = dialogView.findViewById<TextView>(R.id.tvUserEmailDetail)
        val tvUserPhone = dialogView.findViewById<TextView>(R.id.tvUserPhone)
        val tvUserRegion = dialogView.findViewById<TextView>(R.id.tvUserRegion)

        val tvFarmsCount = dialogView.findViewById<TextView>(R.id.tvFarmsCount)
        val layoutFarmsEmpty = dialogView.findViewById<LinearLayout>(R.id.layoutFarmsEmpty)

        val tvBusinessesCount = dialogView.findViewById<TextView>(R.id.tvBusinessesCount)
        val layoutBusinessesEmpty = dialogView.findViewById<LinearLayout>(R.id.layoutBusinessesEmpty)

        val tvCertificationsCount = dialogView.findViewById<TextView>(R.id.tvCertificationsCount)
        val layoutCertificationsEmpty = dialogView.findViewById<LinearLayout>(R.id.layoutCertificationsEmpty)

        val layoutExpiryWarning = dialogView.findViewById<LinearLayout>(R.id.layoutExpiryWarning)
        val tvExpiryWarning = dialogView.findViewById<TextView>(R.id.tvExpiryWarning)


        // Show dialog first (so UI is ready)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()

        lifecycleScope.launch {
            try {
                val documentSnapshot = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                // Get user data
                val name = documentSnapshot.getString("name") ?: "User"
                val email = documentSnapshot.getString("email") ?: ""
                val phone = documentSnapshot.getString("phone") ?: ""
                val avatar = documentSnapshot.getString("avatar") ?: "👤"
                val region = documentSnapshot.getString("region") ?: "Not Set"

                // Update UI
                tvAvatar?.text = avatar
                tvUserName?.text = name
                tvUserEmail?.text = email
                tvUserEmailDetail?.text = email
                tvUserPhone?.text = phone
                tvUserRegion?.text = region

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error loading profile: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Load farms
        loadFarms(dialogView, tvFarmsCount, layoutFarmsEmpty,userId)

        // Load businesses
        loadBusinesses(dialogView, tvBusinessesCount, layoutBusinessesEmpty,userId)

        // Load certifications
        loadCertifications(dialogView, tvCertificationsCount, layoutCertificationsEmpty, layoutExpiryWarning, tvExpiryWarning,userId)

    }


    private fun showProfileScreen() {
        currentScreen = "profile"
        binding.tvAppTitle.text = "My Profile"

        // Inflate Profile layout into fragment container
        binding.fragmentContainer.removeAllViews()
        val profileView = layoutInflater.inflate(R.layout.fragment_profile, binding.fragmentContainer, false)
        binding.fragmentContainer.addView(profileView)

        // Setup Profile screen content
        setupProfileScreen(profileView)
    }

    private fun setupProfileScreen(view: View) {
        val currentUser = authRepository.getCurrentUser() ?: return

        // Find views
        val tvAvatar = view.findViewById<android.widget.TextView>(R.id.tvAvatar)
        val tvUserName = view.findViewById<android.widget.TextView>(R.id.tvUserName)
        val tvUserEmail = view.findViewById<android.widget.TextView>(R.id.tvUserEmail)
        val tvUserEmailDetail = view.findViewById<android.widget.TextView>(R.id.tvUserEmailDetail)
        val tvUserPhone = view.findViewById<android.widget.TextView>(R.id.tvUserPhone)
        val tvUserRegion = view.findViewById<android.widget.TextView>(R.id.tvUserRegion)
        val tvFarmsCount = view.findViewById<android.widget.TextView>(R.id.tvFarmsCount)
        val tvBusinessesCount = view.findViewById<android.widget.TextView>(R.id.tvBusinessesCount)
        val tvCertificationsCount = view.findViewById<android.widget.TextView>(R.id.tvCertificationsCount)
        val layoutFarmsEmpty = view.findViewById<LinearLayout>(R.id.layoutFarmsEmpty)
        val layoutBusinessesEmpty = view.findViewById<LinearLayout>(R.id.layoutBusinessesEmpty)
        val layoutCertificationsEmpty = view.findViewById<LinearLayout>(R.id.layoutCertificationsEmpty)
        val layoutExpiryWarning = view.findViewById<LinearLayout>(R.id.layoutExpiryWarning)
        val tvExpiryWarning = view.findViewById<android.widget.TextView>(R.id.tvExpiryWarning)
        val btnEditProfile = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnEditProfile)
        val btnAddFarm = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddFarm)
        val btnAddBusiness = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddBusiness)
        val btnAddCertification = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddCertification)
        val btnLogout = view.findViewById<android.widget.LinearLayout>(R.id.btnLogout)
        val btnChangePassword = view.findViewById<android.widget.LinearLayout>(R.id.btnChangePassword)

        // Load user data
        lifecycleScope.launch {
            try {
                val documentSnapshot = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()

                // Get user data
                val name = documentSnapshot.getString("name") ?: "User"
                val email = documentSnapshot.getString("email") ?: ""
                val phone = documentSnapshot.getString("phone") ?: ""
                val avatar = documentSnapshot.getString("avatar") ?: "👤"
                val region = documentSnapshot.getString("region") ?: "Not Set"

                // Update UI
                tvAvatar?.text = avatar
                tvUserName?.text = name
                tvUserEmail?.text = email
                tvUserEmailDetail?.text = email
                tvUserPhone?.text = phone
                tvUserRegion?.text = region

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error loading profile: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Load farms
        loadFarms(view, tvFarmsCount, layoutFarmsEmpty,null)

        // Load businesses
        loadBusinesses(view, tvBusinessesCount, layoutBusinessesEmpty,null)

        // Load certifications
        loadCertifications(view, tvCertificationsCount, layoutCertificationsEmpty, layoutExpiryWarning, tvExpiryWarning,null)

        // Setup click listeners
        btnEditProfile?.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        btnAddFarm?.setOnClickListener {
            val intent = Intent(this, AddFarmActivity::class.java)
            startActivity(intent)
        }

        btnAddBusiness?.setOnClickListener {
            val intent = Intent(this, AddBusinessActivity::class.java)
            startActivity(intent)
        }

        btnAddCertification?.setOnClickListener {
            val intent = Intent(this, AddCertificationActivity::class.java)
            startActivity(intent)
        }

        btnChangePassword?.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        btnLogout?.setOnClickListener {
            authRepository.logout()
            navigateToLogin()
        }
    }

    private fun showAnnouncementsScreen() {
        currentScreen = "announcements"
        binding.tvAppTitle.text = "Announcements"

        // Inflate announcements layout
        val announcementsView = layoutInflater.inflate(R.layout.fragment_announcements, binding.fragmentContainer, false)

        // Clear container and add announcements view
        binding.fragmentContainer.removeAllViews()
        binding.fragmentContainer.addView(announcementsView)

        // Setup announcements screen
        setupAnnouncementsScreen(announcementsView)
    }

    private var currentAnnouncementsFilter = "All"

    private fun setupAnnouncementsScreen(view: View) {
        val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewAnnouncements)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        val layoutEmpty = view.findViewById<LinearLayout>(R.id.layoutEmpty)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val fabCreateAnnouncement = view.findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(R.id.fabCreateAnnouncement)
        val chipGroupCategories = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupCategories)

        val currentUserId = authRepository.getCurrentUser()?.uid ?: ""

        // Setup RecyclerView
        val adapter = AnnouncementsAdapter(
            announcements = emptyList(),
            currentUserId = currentUserId,
            onAnnouncementClick = { announcement ->
                val intent = Intent(this, AnnouncementDetailsActivity::class.java)
                intent.putExtra("ANNOUNCEMENT_ID", announcement.id)
                startActivity(intent)
            },
            onLikeClick = { announcement ->
                likeAnnouncement(announcement, view)
            },
            onCommentClick = { announcement ->
                val intent = Intent(this, AnnouncementDetailsActivity::class.java)
                intent.putExtra("ANNOUNCEMENT_ID", announcement.id)
                startActivity(intent)
            },
            onMoreClick = { announcement ->
                showAnnouncementMoreOptionsDialog(announcement, view)
            }
        )

        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Setup FAB
        fabCreateAnnouncement.setOnClickListener {
            val intent = Intent(this, CreateAnnouncementActivity::class.java)
            startActivity(intent)
        }

        // Setup swipe refresh
        swipeRefresh.setOnRefreshListener {
            loadAnnouncements(view, adapter, layoutEmpty, progressBar, swipeRefresh)
        }

        // Setup filter chips
        chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                return@setOnCheckedStateChangeListener
            }

            val checkedChip = view.findViewById<Chip>(checkedIds[0])
            currentAnnouncementsFilter = when (checkedChip.id) {
                R.id.chipAll -> "All"
                R.id.chipMyPost -> "My Post"
                R.id.chipGeneral -> "General Updates"
                R.id.chipMarket -> "Market Prices"
                R.id.chipEquipment -> "Equipment & Tools"
                R.id.chipTips -> "Growing Tips"
                R.id.chipCollaboration -> "Collaboration"
                R.id.chipEvents -> "Events & Workshops"
                R.id.chipAlerts -> "Alerts & Warnings"
                R.id.chipSuccess -> "Success Stories"
                else -> "All"
            }

            loadAnnouncements(view, adapter, layoutEmpty, progressBar, swipeRefresh)
        }

        // Load announcements
        loadAnnouncements(view, adapter, layoutEmpty, progressBar, swipeRefresh)
    }

    private fun loadAnnouncements(
        view: View,
        adapter: AnnouncementsAdapter,
        layoutEmpty: LinearLayout,
        progressBar: ProgressBar,
        swipeRefresh: SwipeRefreshLayout
    ) {
        progressBar.visibility = View.VISIBLE

        val announcementRepository = AnnouncementRepository()

        lifecycleScope.launch {
            val result = if (currentAnnouncementsFilter == "All") {
                announcementRepository.getAllAnnouncements()
            } else if (currentAnnouncementsFilter == "My Post"){
                announcementRepository.getUserAnnouncements()
            } else {
                announcementRepository.getAnnouncementsByCategory(currentAnnouncementsFilter)
            }

            swipeRefresh.isRefreshing = false
            progressBar.visibility = View.GONE

            result.fold(
                onSuccess = { announcements ->
                    if (announcements.isEmpty()) {
                        layoutEmpty.visibility = View.VISIBLE
                        view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewAnnouncements).visibility = View.GONE
                    } else {
                        layoutEmpty.visibility = View.GONE
                        view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewAnnouncements).visibility = View.VISIBLE
                        adapter.updateAnnouncements(announcements)
                    }
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@MainActivity,
                        "Error loading announcements: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun likeAnnouncement(announcement: Announcement, view: View) {
        val announcementRepository = AnnouncementRepository()

        lifecycleScope.launch {
            val result = announcementRepository.likeAnnouncement(announcement.id)

            result.fold(
                onSuccess = {
                    // Reload announcements
                    val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewAnnouncements)
                    val adapter = recyclerView.adapter as? AnnouncementsAdapter
                    val layoutEmpty = view.findViewById<LinearLayout>(R.id.layoutEmpty)
                    val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
                    val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)

                    if (adapter != null) {
                        loadAnnouncements(view, adapter, layoutEmpty, progressBar, swipeRefresh)
                    }
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@MainActivity,
                        "Error: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun showAnnouncementMoreOptionsDialog(announcement: Announcement, view: View) {
        val options = arrayOf("Delete")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Announcement Options")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> showDeleteAnnouncementConfirmationDialog(announcement, view)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun showDeleteAnnouncementConfirmationDialog(announcement: Announcement, view: View) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Announcement")
            .setMessage("Are you sure you want to delete this announcement?\n\nThis action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteAnnouncement(announcement, view)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteAnnouncement(announcement: Announcement, view: View) {
        val announcementRepository = AnnouncementRepository()

        lifecycleScope.launch {
            val result = announcementRepository.deleteAnnouncement(announcement.id)

            result.fold(
                onSuccess = {
                    Toast.makeText(
                        this@MainActivity,
                        "Announcement deleted",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Reload announcements
                    val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewAnnouncements)
                    val adapter = recyclerView.adapter as? AnnouncementsAdapter
                    val layoutEmpty = view.findViewById<LinearLayout>(R.id.layoutEmpty)
                    val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
                    val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)

                    if (adapter != null) {
                        loadAnnouncements(view, adapter, layoutEmpty, progressBar, swipeRefresh)
                    }
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@MainActivity,
                        "Error: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun loadFarms(
        profileView: View,
        tvFarmsCount: android.widget.TextView?,
        layoutFarmsEmpty: LinearLayout?,
        userId: String? = null
    ) {
        val farmRepository = FarmRepository()
        val farmsListContainer = profileView.findViewById<LinearLayout>(R.id.farmsListContainer)

        lifecycleScope.launch {
            val result = farmRepository.getUserFarms(userId)

            result.fold(
                onSuccess = { farms ->
                    tvFarmsCount?.text = farms.size.toString()

                    if (farms.isEmpty()) {
                        layoutFarmsEmpty?.visibility = View.VISIBLE
                        farmsListContainer?.visibility = View.GONE
                    } else {
                        layoutFarmsEmpty?.visibility = View.GONE
                        farmsListContainer?.visibility = View.VISIBLE

                        // Clear previous farms
                        farmsListContainer?.removeAllViews()

                        // Add each farm card
                        farms.forEach { farm ->
                            val farmCardView = layoutInflater.inflate(
                                R.layout.item_farm_card,
                                farmsListContainer,
                                false
                            )

                            setupFarmCard(farmCardView, farm,userId)
                            farmsListContainer?.addView(farmCardView)
                        }
                    }
                },
                onFailure = { exception ->
                    Log.e("FARM_ERROR", "Error loading farms", exception)

                    Toast.makeText(
                        this@MainActivity,
                        "Error loading farms: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun setupFarmCard(cardView: View, farm: Farm,userId: String?=null) {
        // Find views
        val tvFarmName = cardView.findViewById<android.widget.TextView>(R.id.tvFarmName)
        val tvFarmLocation = cardView.findViewById<android.widget.TextView>(R.id.tvFarmLocation)
        val layoutGPS = cardView.findViewById<LinearLayout>(R.id.layoutGPS)
        val tvGPS = cardView.findViewById<android.widget.TextView>(R.id.tvGPS)
        val tvTotalSize = cardView.findViewById<android.widget.TextView>(R.id.tvTotalSize)
        val tvFarmerType = cardView.findViewById<android.widget.TextView>(R.id.tvFarmerType)
        val varietiesChipGroup = cardView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.varietiesChipGroup)
        val btnEditFarm = cardView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnEditFarm)
        val btnDeleteFarm = cardView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDeleteFarm)

        // Set data
        tvFarmName?.text = farm.name
        tvFarmLocation?.text = farm.location
        tvTotalSize?.text = "${farm.totalSize} hectares"
        tvFarmerType?.text = farm.farmerType

        // Show GPS if available
        if (farm.gpsLatitude.isNotEmpty() && farm.gpsLongitude.isNotEmpty()) {
            layoutGPS?.visibility = View.VISIBLE
            tvGPS?.text = "GPS: ${farm.gpsLatitude}, ${farm.gpsLongitude}"
        } else {
            layoutGPS?.visibility = View.GONE
        }

        // Add variety chips
        varietiesChipGroup?.removeAllViews()
        farm.varieties.forEach { variety ->
            val chip = com.google.android.material.chip.Chip(this)
            chip.text = "${variety.variety} • ${variety.areaSize} hectares"
            chip.setChipBackgroundColorResource(android.R.color.white)
            chip.setTextColor(getColor(R.color.green_primary))
            chip.chipStrokeWidth = 4f
            chip.setChipStrokeColorResource(R.color.green_primary)
            chip.isClickable = false
            chip.isCheckable = false
            varietiesChipGroup?.addView(chip)
        }

        // 2. Control Visibility based on isEditable
        if (userId == null) {
            // Edit button
            btnEditFarm?.setOnClickListener {
                val intent = Intent(this, EditFarmActivity::class.java)
                intent.putExtra("FARM_ID", farm.id)
                startActivity(intent)
            }
            // Delete button
            btnDeleteFarm?.setOnClickListener {
                showDeleteFarmDialog(farm)
            }
        } else {
            // Hide buttons if viewing someone else's profile
            btnEditFarm?.visibility = View.GONE
            btnDeleteFarm?.visibility = View.GONE
        }

    }

    private fun showDeleteFarmDialog(farm:  Farm) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Farm")
            .setMessage("Are you sure you want to delete \"${farm.name}\"?\n\nThis action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteFarm(farm.id)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteFarm(farmId: String) {
        val farmRepository = FarmRepository()

        lifecycleScope.launch {
            val result = farmRepository.deleteFarm(farmId)

            result.fold(
                onSuccess = {
                    Toast.makeText(
                        this@MainActivity,
                        "Farm deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Reload profile to update farms list
                    showProfileScreen()
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@MainActivity,
                        "Error deleting farm: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loadBusinesses(
        profileView: View,
        tvBusinessesCount: android.widget.TextView?,
        layoutBusinessesEmpty: LinearLayout?,
        userId: String?=null
    ) {
        val businessRepository = BusinessRepository()
        val businessesListContainer = profileView.findViewById<LinearLayout>(R.id.businessesListContainer)

        lifecycleScope.launch {
            val result = businessRepository.getUserBusinesses(userId)

            result.fold(
                onSuccess = { businesses ->
                    tvBusinessesCount?.text = businesses.size.toString()

                    if (businesses.isEmpty()) {
                        layoutBusinessesEmpty?.visibility = View.VISIBLE
                        businessesListContainer?.visibility = View.GONE
                    } else {
                        layoutBusinessesEmpty?.visibility = View.GONE
                        businessesListContainer?.visibility = View.VISIBLE

                        // Clear previous businesses
                        businessesListContainer?.removeAllViews()

                        // Add each business card
                        businesses.forEach { business ->
                            val businessCardView = layoutInflater.inflate(
                                R.layout.item_business_card,
                                businessesListContainer,
                                false
                            )

                            setupBusinessCard(businessCardView, business,userId)
                            businessesListContainer?.addView(businessCardView)
                        }
                    }
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@MainActivity,
                        "Error loading businesses: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun setupBusinessCard(cardView: View, business: Business,userId:String?=null) {
        // Find views
        val tvBusinessName = cardView.findViewById<android.widget.TextView>(R.id.tvBusinessName)
        val tvBusinessType = cardView.findViewById<android.widget.TextView>(R.id.tvBusinessType)
        val tvBusinessLocation = cardView.findViewById<android.widget.TextView>(R.id.tvBusinessLocation)
        val tvBusinessPhone = cardView.findViewById<android.widget.TextView>(R.id.tvBusinessPhone)
        val layoutOperatingHours = cardView.findViewById<LinearLayout>(R.id.layoutOperatingHours)
        val tvOperatingHours = cardView.findViewById<android.widget.TextView>(R.id.tvOperatingHours)
        val layoutDescription = cardView.findViewById<LinearLayout>(R.id.layoutDescription)
        val tvDescription = cardView.findViewById<android.widget.TextView>(R.id.tvDescription)
        val layoutGPS = cardView.findViewById<LinearLayout>(R.id.layoutGPS)
        val tvGPS = cardView.findViewById<android.widget.TextView>(R.id.tvGPS)
        val btnEditBusiness = cardView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnEditBusiness)
        val btnDeleteBusiness = cardView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDeleteBusiness)

        // Set data
        tvBusinessName?.text = business.name
        tvBusinessType?.text = business.type
        tvBusinessLocation?.text = business.location
        tvBusinessPhone?.text = business.phone

        // Show operating hours if available
        if (business.operatingHours.isNotEmpty()) {
            layoutOperatingHours?.visibility = View.VISIBLE
            tvOperatingHours?.text = business.operatingHours
        } else {
            layoutOperatingHours?.visibility = View.GONE
        }

        // Show description if available
        if (business.description.isNotEmpty()) {
            layoutDescription?.visibility = View.VISIBLE
            tvDescription?.text = business.description
        } else {
            layoutDescription?.visibility = View.GONE
        }

        // Show GPS if available
        if (business.gpsLatitude.isNotEmpty() && business.gpsLongitude.isNotEmpty()) {
            layoutGPS?.visibility = View.VISIBLE
            tvGPS?.text = "GPS: ${business.gpsLatitude}, ${business.gpsLongitude}"
        } else {
            layoutGPS?.visibility = View.GONE
        }
        // 2. Control Visibility based on isEditable
        if (userId == null) {
            // Edit button
            btnEditBusiness?.setOnClickListener {
                val intent = Intent(this, EditBusinessActivity::class.java)
                intent.putExtra("BUSINESS_ID", business.id)
                startActivity(intent)
            }
            // Delete button
            btnDeleteBusiness?.setOnClickListener {
                showDeleteBusinessDialog(business)
            }
        } else {
            // Hide buttons if viewing someone else's profile
            btnEditBusiness?.visibility = View.GONE
            btnDeleteBusiness?.visibility = View.GONE
        }
    }

    private fun showDeleteBusinessDialog(business: Business) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Business")
            .setMessage("Are you sure you want to delete \"${business.name}\"?\n\nThis action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteBusiness(business.id)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteBusiness(businessId: String) {
        val businessRepository = BusinessRepository()

        lifecycleScope.launch {
            val result = businessRepository.deleteBusiness(businessId)

            result.fold(
                onSuccess = {
                    Toast.makeText(
                        this@MainActivity,
                        "Business deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Reload profile to update businesses list
                    showProfileScreen()
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@MainActivity,
                        "Error deleting business: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun loadCertifications(
        profileView: View,
        tvCertificationsCount: android.widget.TextView?,
        layoutCertificationsEmpty: LinearLayout?,
        layoutExpiryWarning: LinearLayout?,
        tvExpiryWarning: android.widget.TextView?,
        userId: String? = null
    ) {
        val certificationRepository = CertificationRepository()
        val certificationsListContainer = profileView.findViewById<LinearLayout>(R.id.certificationsListContainer)

        lifecycleScope.launch {
            val result = certificationRepository.getUserCertifications(userId)

            result.fold(
                onSuccess = { certifications ->
                    tvCertificationsCount?.text = certifications.size.toString()

                    if (certifications.isEmpty()) {
                        layoutCertificationsEmpty?.visibility = View.VISIBLE
                        certificationsListContainer?.visibility = View.GONE
                        layoutExpiryWarning?.visibility = View.GONE
                    } else {
                        layoutCertificationsEmpty?.visibility = View.GONE
                        certificationsListContainer?.visibility = View.VISIBLE

                        // Clear previous certifications
                        certificationsListContainer?.removeAllViews()

                        // Check for expiring certifications
                        val expiringSoon = certifications.filter { cert ->
                            val days = cert.daysUntilExpiry()
                            days in 0..90
                        }

                        if (expiringSoon.isNotEmpty()) {
                            layoutExpiryWarning?.visibility = View.VISIBLE
                            val warningText = if (expiringSoon.size == 1) {
                                "1 certification expiring soon"
                            } else {
                                "${expiringSoon.size} certifications expiring soon"
                            }
                            tvExpiryWarning?.text = warningText
                        } else {
                            layoutExpiryWarning?.visibility = View.GONE
                        }

                        // Add each certification card
                        certifications.forEach { certification ->
                            val certificationCardView = layoutInflater.inflate(
                                R.layout.item_certification_card,
                                certificationsListContainer,
                                false
                            )

                            setupCertificationCard(certificationCardView, certification,userId)
                            certificationsListContainer?.addView(certificationCardView)
                        }
                    }
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@MainActivity,
                        "Error loading certifications: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun setupCertificationCard(cardView: View, certification: Certification, userId:String?=null) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        // Find views
        val certCardBackground = cardView.findViewById<LinearLayout>(R.id.certCardBackground)
        val tvCertificationType = cardView.findViewById<android.widget.TextView>(R.id.tvCertificationType)
        val tvCertificateNumber = cardView.findViewById<android.widget.TextView>(R.id.tvCertificateNumber)
        val tvIssuingBody = cardView.findViewById<android.widget.TextView>(R.id.tvIssuingBody)
        val tvIssuedDate = cardView.findViewById<android.widget.TextView>(R.id.tvIssuedDate)
        val tvExpiryDate = cardView.findViewById<android.widget.TextView>(R.id.tvExpiryDate)
        val statusBadge = cardView.findViewById<LinearLayout>(R.id.statusBadge)
        val tvStatusIcon = cardView.findViewById<android.widget.TextView>(R.id.tvStatusIcon)
        val tvStatus = cardView.findViewById<android.widget.TextView>(R.id.tvStatus)
        val layoutNotes = cardView.findViewById<LinearLayout>(R.id.layoutNotes)
        val tvNotes = cardView.findViewById<android.widget.TextView>(R.id.tvNotes)
        val divider = cardView.findViewById<View>(R.id.divider)
        val btnEditCertification = cardView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnEditCertification)
        val btnDeleteCertification = cardView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDeleteCertification)

        // Set data
        tvCertificationType?.text = certification.type
        tvCertificateNumber?.text = "Cert #: ${certification.certificateNumber}"
        tvIssuingBody?.text = certification.issuingBody
        tvIssuedDate?.text = dateFormat.format(Date(certification.issuedDate))
        tvExpiryDate?.text = dateFormat.format(Date(certification.expiryDate))

        // Show notes if available
        if (certification.notes.isNotEmpty()) {
            layoutNotes?.visibility = View.VISIBLE
            tvNotes?.text = certification.notes
        } else {
            layoutNotes?.visibility = View.GONE
        }

        // Set status
        tvStatusIcon?.text = certification.getStatusIcon()
        tvStatus?.text = certification.getStatusText()

        // Set status badge color
        try {
            val statusColor = Color.parseColor(certification.getStatusColor())
            statusBadge?.setBackgroundColor(statusColor)

            // Set card border color based on status
            val card = cardView as? com.google.android.material.card.MaterialCardView
            card?.strokeColor = statusColor

            // Set divider color
            divider?.setBackgroundColor(statusColor)
        } catch (e: Exception) {
            // Fallback to default colors if parsing fails
        }

        if(userId ==null){
            // Edit button
            btnEditCertification?.setOnClickListener {
                val intent = Intent(this, EditCertificationActivity::class.java)
                intent.putExtra("CERTIFICATION_ID", certification.id)
                startActivity(intent)
            }

            // Delete button
            btnDeleteCertification?.setOnClickListener {
                showDeleteCertificationDialog(certification)
            }
        }
        else{
            btnEditCertification.visibility=View.GONE
            btnDeleteCertification.visibility=View.GONE
        }

    }

    private fun showDeleteCertificationDialog(certification: Certification) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Certification")
            .setMessage("Are you sure you want to delete \"${certification.type}\"?\n\nCertificate #: ${certification.certificateNumber}\n\nThis action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteCertification(certification.id)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteCertification(certificationId: String) {
        val certificationRepository = CertificationRepository()

        lifecycleScope.launch {
            val result = certificationRepository.deleteCertification(certificationId)

            result.fold(
                onSuccess = {
                    Toast.makeText(
                        this@MainActivity,
                        "Certification deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Reload profile to update certifications list
                    showProfileScreen()
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@MainActivity,
                        "Error deleting certification: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
}