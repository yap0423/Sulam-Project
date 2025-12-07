package com.example.entrepreneurapp.ui.planner

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.entrepreneurapp.R
import com.example.entrepreneurapp.databinding.ActivityProductionPlannerBinding
import com.example.entrepreneurapp.models.*
import com.example.entrepreneurapp.repository.AuthRepository
import com.example.entrepreneurapp.repository.HarvestRepository
import com.example.entrepreneurapp.MainActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.tasks.await

class ProductionPlannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductionPlannerBinding
    private val harvestRepository = HarvestRepository()
    private val authRepository = AuthRepository()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var myHarvestsAdapter: MyHarvestsAdapter
    private lateinit var groupMembersAdapter: GroupMembersAdapter
    private lateinit var conflictsAdapter: ConflictsAdapter
    private lateinit var weeklyYieldAdapter: WeeklyYieldAdapter

    private var currentRegion = ""
    private var currentUserId = ""
    private var currentTab = "timeline"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductionPlannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserData()
        setupToolbar()
        setupTabs()
        setupRecyclerViews()
        setupClickListeners()
        setupBottomNavigation()
        showTimelineTab()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Production Planner"
        // No back button since we have bottom navigation
    }

    private fun setupTabs() {
        binding.tabTimeline.setOnClickListener {
            currentTab = "timeline"
            showTimelineTab()
        }

        binding.tabGroup.setOnClickListener {
            currentTab = "group"
            showGroupTab()
        }

        binding.tabConflicts.setOnClickListener {
            currentTab = "conflicts"
            showConflictsTab()
        }

        binding.tabReports.setOnClickListener {
            currentTab = "reports"
            showReportsTab()
        }
    }

    private fun setupRecyclerViews() {
        // My Harvests RecyclerView
        myHarvestsAdapter = MyHarvestsAdapter(
            onEditClick = { harvest ->
                val intent = Intent(this, EditHarvestActivity::class.java)
                intent.putExtra("harvestId", harvest.id)
                startActivity(intent)
            },
            onDeleteClick = { harvest ->
                deleteHarvest(harvest)
            }
        )
        binding.rvMyHarvests.apply {
            layoutManager = LinearLayoutManager(this@ProductionPlannerActivity)
            adapter = myHarvestsAdapter
        }

        // Group Members RecyclerView
        groupMembersAdapter = GroupMembersAdapter()
        binding.rvGroupMembers.apply {
            layoutManager = LinearLayoutManager(this@ProductionPlannerActivity)
            adapter = groupMembersAdapter
        }

        // Conflicts RecyclerView
        conflictsAdapter = ConflictsAdapter { conflict ->
            openConflictChat(conflict)
        }
        binding.rvConflicts.apply {
            layoutManager = LinearLayoutManager(this@ProductionPlannerActivity)
            adapter = conflictsAdapter
        }

        // Weekly Yield RecyclerView
        weeklyYieldAdapter = WeeklyYieldAdapter(currentRegion)
        binding.rvWeeklyYield.apply {
            layoutManager = LinearLayoutManager(this@ProductionPlannerActivity)
            adapter = weeklyYieldAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnAddHarvest.setOnClickListener {
            val intent = Intent(this, AddHarvestActivity::class.java)
            startActivity(intent)
        }

        binding.btnViewConflicts.setOnClickListener {
            currentTab = "conflicts"
            showConflictsTab()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_planner

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("show_home", true)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_announcements -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("show_announcements", true)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_planner -> {
                    // Already on planner
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("show_profile", true)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            val result = authRepository.getUserProfile()
            result.onSuccess { user ->
                currentUserId = user.uid
                currentRegion = user.region

                android.util.Log.d("PlannerActivity", "User loaded: ${user.name}, Region: ${user.region}")

                // Update adapter's region
                weeklyYieldAdapter.updateRegion(currentRegion)

                binding.tvCurrentRegion.text = currentRegion

                loadMyHarvests()

                // Only load group data if we're on group tab
                if (currentTab == "group") {
                    loadGroupData()
                }
            }.onFailure { exception ->
                Toast.makeText(this@ProductionPlannerActivity, "Error loading user: ${exception.message}", Toast.LENGTH_SHORT).show()
                android.util.Log.e("PlannerActivity", "Error loading user", exception)
            }
        }
    }

    private fun loadMyHarvests() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = harvestRepository.getUserHarvests()
            binding.progressBar.visibility = View.GONE

            result.onSuccess { harvests ->
                if (harvests.isEmpty()) {
                    binding.tvEmptyHarvests.visibility = View.VISIBLE
                    binding.rvMyHarvests.visibility = View.GONE
                } else {
                    binding.tvEmptyHarvests.visibility = View.GONE
                    binding.rvMyHarvests.visibility = View.VISIBLE
                    myHarvestsAdapter.submitList(harvests)

                    // Update reports
                    updateMyStats(harvests)
                }
            }.onFailure { exception ->
                Toast.makeText(this@ProductionPlannerActivity, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadGroupData() {
        if (currentRegion.isEmpty()) {
            Toast.makeText(this, "Region not loaded yet", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                android.util.Log.d("PlannerActivity", "Loading group data for region: $currentRegion")

                val usersSnapshot = db.collection("users")
                    .whereEqualTo("region", currentRegion)
                    .get()
                    .await()

                android.util.Log.d("PlannerActivity", "Found ${usersSnapshot.documents.size} users in region")

                val members = mutableListOf<GroupMember>()

                usersSnapshot.documents.forEach { doc ->
                    val user = doc.toObject(User::class.java)
                    if (user != null) {
                        android.util.Log.d("PlannerActivity", "Adding member: ${user.name}")
                        members.add(
                            GroupMember(
                                userId = user.uid,
                                userName = user.name,
                                userAvatar = user.avatar,
                                region = user.region,
                                businessName = user.businessName
                            )
                        )
                    }
                }

                // If no members found, at least show current user
                if (members.isEmpty()) {
                    val result = authRepository.getUserProfile()
                    result.onSuccess { user ->
                        members.add(
                            GroupMember(
                                userId = user.uid,
                                userName = user.name,
                                userAvatar = user.avatar,
                                region = user.region,
                                businessName = user.businessName
                            )
                        )
                    }
                }

                groupMembersAdapter.submitList(members)
                binding.tvGroupMemberCount.text = "${members.size} farmer${if (members.size != 1) "s" else ""}"

                android.util.Log.d("PlannerActivity", "Group members loaded: ${members.size}")

                // Now load harvest data for conflict detection and charts
                loadHarvestDataForGroup()

            } catch (e: Exception) {
                android.util.Log.e("PlannerActivity", "Error loading group", e)
                Toast.makeText(this@ProductionPlannerActivity, "Error loading group: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadHarvestDataForGroup() {
        lifecycleScope.launch {
            val result = harvestRepository.getRegionHarvests(currentRegion)
            result.onSuccess { allHarvests ->
                android.util.Log.d("PlannerActivity", "Loaded ${allHarvests.size} harvests for region")

                // Detect conflicts
                val conflicts = detectConflicts(allHarvests)
                conflictsAdapter.submitList(conflicts)

                if (conflicts.any { it.riskLevel == "high" }) {
                    binding.cardHighRisk.visibility = View.VISIBLE
                    binding.tvHighRiskDays.text = "${conflicts.count { it.riskLevel == "high" }} day(s) have excessive projected yield."
                } else {
                    binding.cardHighRisk.visibility = View.GONE
                }

                // Calculate weekly yields
                val weeklyYields = calculateWeeklyYields(allHarvests)
                weeklyYieldAdapter.submitList(weeklyYields)

                // Update group stats
                updateGroupStats(allHarvests)
            }.onFailure {
                android.util.Log.e("PlannerActivity", "Error loading harvests", it)
                // Even if harvest data fails, we still show members
                binding.cardHighRisk.visibility = View.GONE
                weeklyYieldAdapter.submitList(emptyList())
                updateGroupStats(emptyList())
            }
        }
    }

    private fun updateMyStats(harvests: List<HarvestSchedule>) {
        val totalYield = harvests.sumOf { it.estimatedYield }
        binding.tvMyTotalYield.text = "${totalYield.toInt()} tonnes"
        binding.tvMyActiveCrops.text = harvests.size.toString()
    }

    private fun updateGroupStats(harvests: List<HarvestSchedule>) {
        val totalYield = harvests.sumOf { it.estimatedYield }
        val memberCount = binding.tvGroupMemberCount.text.toString().split(" ")[0].toIntOrNull() ?: 1

        binding.tvGroupTotalYield.text = "${totalYield.toInt()} tonnes"
        binding.tvGroupMemberCountStats.text = memberCount.toString()

        if (harvests.isNotEmpty()) {
            // Yield by farmer
            val yieldByUser = harvests.groupBy { it.userId }
                .map { (_, userHarvests) ->
                    val user = userHarvests.first()
                    val userName = if (user.userId == currentUserId) "You" else user.userName
                    val yield = userHarvests.sumOf { it.estimatedYield }.toInt()
                    val dots = ".".repeat(maxOf(0, 25 - userName.length))
                    "$userName$dots$yield tonnes"
                }

            binding.tvYieldByFarmer.text = if (yieldByUser.isEmpty()) {
                "No harvest data available yet"
            } else {
                yieldByUser.joinToString("\n")
            }
        } else {
            binding.tvYieldByFarmer.text = "No harvest data available yet"
        }
    }

    private fun detectConflicts(harvests: List<HarvestSchedule>): List<HarvestConflict> {

        // Only include future/pending harvests
        val pendingHarvests = harvests.filter { it.getDaysUntilHarvest() >= 0 }

        val conflicts = mutableListOf<HarvestConflict>()

        // Group by harvest start date
        val dateGroups = pendingHarvests.groupBy { harvest ->
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(harvest.harvestStartDate.toDate())
        }

        dateGroups.forEach { (dateStr, dayHarvests) ->
            val totalYield = dayHarvests.sumOf { it.estimatedYield }
            val riskLevel = getRiskLevel(totalYield)

            // 👉 Calculate unique farmers FOR THIS DAY ONLY
            val farmersAffected = dayHarvests.map { it.userId }.distinct()

            // Conflict condition: more than 1 farmers AND risk is high
            if (farmersAffected.size > 1 && riskLevel == "high") {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                conflicts.add(
                    HarvestConflict(
                        date = Timestamp(date!!),
                        totalYield = totalYield,
                        riskLevel = riskLevel,
                        farmersAffected = dayHarvests.map { it.userId },
                        schedules = dayHarvests
                    )
                )
            }
        }
        return conflicts.sortedByDescending { it.totalYield }
    }

    fun getRiskLevel(totalYield: Double): String {
        val weeklyAvg = when (currentRegion) {
            "Kluang, Johor" -> 3340.0
            "Kubang Pasu, Kedah" -> 46.57
            "Pasir Puteh, Kelantan" -> 20.84
            "Alor Gajah, Melaka" -> 36.2
            "Kuala Pilah, Negeri Sembilan" -> 44.06
            "Rompin, Pahang" -> 840.25
            "Seberang Perai Selatan, Pulau Pinang" -> 70.19
            "Perak Tengah, Perak" -> 85.92
            "Perlis, Perlis" -> 57.34
            "Kuala Langat, Selangor" -> 126.39
            "Setiu, Terengganu" -> 36.31
            "Tuaran, Sabah" -> 207.12
            "Samarahan, Sarawak" -> 189.66
            else -> 1000.0
        }

        return when {
            totalYield > weeklyAvg -> "high"
            totalYield > weeklyAvg / 2 -> "medium"
            else -> "normal"
        }
    }

    private fun calculateWeeklyYields(harvests: List<HarvestSchedule>): List<WeeklyYield> {
        val calendar = Calendar.getInstance()
        val weeklyData = mutableMapOf<String, MutableList<HarvestSchedule>>()

        harvests.forEach { harvest ->
            calendar.time = harvest.harvestStartDate.toDate()
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val weekStart = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            weeklyData.getOrPut(weekStart) { mutableListOf() }.add(harvest)
        }

        return weeklyData.map { (weekStart, weekHarvests) ->
            val totalYield = weekHarvests.sumOf { it.estimatedYield }
            val riskLevel = getRiskLevel(totalYield)

            val calendar = Calendar.getInstance()
            calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(weekStart)!!
            calendar.add(Calendar.DAY_OF_YEAR, 6)
            val weekEnd = SimpleDateFormat("MMM dd", Locale.getDefault()).format(calendar.time)
            val weekStartFormatted = SimpleDateFormat("MMM dd", Locale.getDefault())
                .format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(weekStart)!!)

            WeeklyYield(
                weekStart = weekStartFormatted,
                weekEnd = weekEnd,
                totalYield = totalYield,
                riskLevel = riskLevel,
                farmerCount = weekHarvests.map { it.userId }.distinct().size
            )
        }.sortedBy { it.weekStart }
    }

    private fun deleteHarvest(harvest: HarvestSchedule) {
        lifecycleScope.launch {
            val result = harvestRepository.deleteHarvest(harvest.id)
            result.onSuccess {
                Toast.makeText(this@ProductionPlannerActivity, "Harvest deleted", Toast.LENGTH_SHORT).show()
                loadMyHarvests()
                loadGroupData()
            }.onFailure {
                Toast.makeText(this@ProductionPlannerActivity, "Error deleting harvest", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openConflictChat(conflict: HarvestConflict) {
        val intent = Intent(this, ConflictChatActivity::class.java)
        intent.putExtra("conflictDate", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(conflict.date.toDate()))
        intent.putExtra("totalYield", conflict.totalYield)
        intent.putExtra("riskLevel", conflict.riskLevel)
        intent.putExtra("farmerCount", conflict.farmersAffected.size)
        startActivity(intent)
    }

    private fun showTimelineTab() {
        binding.tabTimeline.setBackgroundResource(R.drawable.tab_selected_background)
        binding.tabGroup.setBackgroundResource(R.drawable.tab_unselected_background)
        binding.tabConflicts.setBackgroundResource(R.drawable.tab_unselected_background)
        binding.tabReports.setBackgroundResource(R.drawable.tab_unselected_background)

        binding.layoutTimeline.visibility = View.VISIBLE
        binding.layoutGroup.visibility = View.GONE
        binding.layoutConflicts.visibility = View.GONE
        binding.layoutReports.visibility = View.GONE
    }

    private fun showGroupTab() {
        binding.tabTimeline.setBackgroundResource(R.drawable.tab_unselected_background)
        binding.tabGroup.setBackgroundResource(R.drawable.tab_selected_background)
        binding.tabConflicts.setBackgroundResource(R.drawable.tab_unselected_background)
        binding.tabReports.setBackgroundResource(R.drawable.tab_unselected_background)

        binding.layoutTimeline.visibility = View.GONE
        binding.layoutGroup.visibility = View.VISIBLE
        binding.layoutConflicts.visibility = View.GONE
        binding.layoutReports.visibility = View.GONE

        // Reload group data when tab is shown
        if (currentRegion.isNotEmpty()) {
            loadGroupData()
        }
    }

    private fun showConflictsTab() {
        binding.tabTimeline.setBackgroundResource(R.drawable.tab_unselected_background)
        binding.tabGroup.setBackgroundResource(R.drawable.tab_unselected_background)
        binding.tabConflicts.setBackgroundResource(R.drawable.tab_selected_background)
        binding.tabReports.setBackgroundResource(R.drawable.tab_unselected_background)

        binding.layoutTimeline.visibility = View.GONE
        binding.layoutGroup.visibility = View.GONE
        binding.layoutConflicts.visibility = View.VISIBLE
        binding.layoutReports.visibility = View.GONE

        loadHarvestDataForGroup()
    }

    private fun showReportsTab() {
        binding.tabTimeline.setBackgroundResource(R.drawable.tab_unselected_background)
        binding.tabGroup.setBackgroundResource(R.drawable.tab_unselected_background)
        binding.tabConflicts.setBackgroundResource(R.drawable.tab_unselected_background)
        binding.tabReports.setBackgroundResource(R.drawable.tab_selected_background)

        binding.layoutTimeline.visibility = View.GONE
        binding.layoutGroup.visibility = View.GONE
        binding.layoutConflicts.visibility = View.GONE
        binding.layoutReports.visibility = View.VISIBLE

        loadMyHarvests()
        loadHarvestDataForGroup()
    }

    override fun onResume() {
        super.onResume()
        if (currentRegion.isNotEmpty()) {
            loadMyHarvests()
            if (currentTab != "timeline") {
                loadGroupData()
            }
        }
    }
}