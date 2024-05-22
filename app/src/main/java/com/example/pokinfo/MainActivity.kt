package com.example.pokinfo

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.example.pokinfo.data.util.sharedPreferences
import com.example.pokinfo.databinding.ActivityMainBinding
import com.example.pokinfo.viewModels.FirebaseViewModel
import com.example.pokinfo.viewModels.SharedViewModel
import com.example.pokinfo.viewModels.factory.ViewModelFactory
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: FirebaseViewModel
    private lateinit var sharedViewModel: SharedViewModel

    private val fabSaveIconRes = R.drawable.baseline_save_as_24
    private val fabAddIconRes = R.drawable.baseline_add_24
    private var isSplashScreenDisplayed = true

    private fun initializeSplashAnimationEnd() {
        Handler(Looper.getMainLooper()).postDelayed({
            isSplashScreenDisplayed = false
        }, 1200)  //
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeSplashAnimationEnd()
        installSplashScreen().setKeepOnScreenCondition {
            isSplashScreenDisplayed
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedViewModel = ViewModelProvider(this)[SharedViewModel::class.java]
        val factory = ViewModelFactory(application, sharedViewModel)
        viewModel = ViewModelProvider(this, factory)[FirebaseViewModel::class.java]

        setSupportActionBar(binding.appBarMain.mainToolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val drawerLayoutHeader = navView.getHeaderView(0)
        val drawerLayoutImage: ImageView = drawerLayoutHeader.findViewById(R.id.ivProfilePic)
        val coordinatorLayout = binding.appBarMain.coordinatorLayout
        val fab = coordinatorLayout.findViewById<FloatingActionButton>(R.id.fabMain)

        drawerLayoutImage.setOnClickListener {
            askIfUserWantsToUpdatePicture()
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_teams, R.id.nav_attacks, R.id.nav_abilities,
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_register, R.id.nav_login -> {
                    supportActionBar?.hide()
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    fab.visibility = View.GONE
                }

                R.id.nav_teambuilder -> {
                    supportActionBar?.show()
                    fab.visibility = View.VISIBLE
                    fab.setImageDrawable(ContextCompat.getDrawable(this, fabSaveIconRes))
                }

                R.id.nav_teams -> {
                    supportActionBar?.show()
                    fab.visibility = View.VISIBLE
                    restoreDrawerNavigation(navView)
                    fab.setImageDrawable(ContextCompat.getDrawable(this, fabAddIconRes))
                }

                R.id.fullScreenAttacksFragment -> {
                    restoreDrawerNavigation(navView)
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    fab.visibility = View.GONE
                }

                R.id.nav_home, R.id.nav_attacks, R.id.nav_abilities -> {
                    supportActionBar?.show()
                    fab.visibility = View.GONE
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                }

                else -> {
                    supportActionBar?.show()
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    fab.visibility = View.GONE
                }
            }
        }

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            }

            override fun onDrawerOpened(drawerView: View) {
                if (!viewModel.isUserLoggedIn()) {
                    Snackbar.make(
                        coordinatorLayout,
                        "You must sign in or register",
                        Snackbar.LENGTH_SHORT
                    )
                        .show()
                    drawerLayout.closeDrawer(drawerView)
                }
            }

            override fun onDrawerClosed(drawerView: View) {
            }

            override fun onDrawerStateChanged(newState: Int) {
            }
        })

        viewModel.user.observe(this) { firebaseUser ->
            if (firebaseUser == null) {
                navController.navigate(R.id.nav_login)
            } else {

                val currentDestination = navController.currentDestination?.id
                // prevents to navigate to home when the dark/white mode is switched (can lead to a activity restart)
                currentDestination?.let { id ->
                    if (id == R.id.nav_login || id == R.id.nav_register) {
                        navController.navigate(R.id.nav_home) //
                    }
                }
                val emailTv = drawerLayoutHeader.findViewById<TextView>(R.id.tvHeaderMail)
                emailTv.text = firebaseUser.email
                viewModel.getProfilePicture {
                    updateProfilePicture(it)
                }
            }
        }


        // Snack bar live-data observation
        sharedViewModel.snackbarSender.observe(this) { message ->
            Snackbar.make(coordinatorLayout, message, Toast.LENGTH_SHORT).show()
        }

        sharedViewModel.snackbarResSender.observe(this) { res ->
            Snackbar.make(coordinatorLayout, getString(res), Snackbar.LENGTH_SHORT).show()
        }

        sharedViewModel.snackBarResWithAction.observe(this) { snackBarWithAction ->
            snackBarWithAction?.let {
                val snackbar = if (it.messageResId != null) {
                    Snackbar.make(coordinatorLayout, it.messageResId, Snackbar.LENGTH_LONG)
                } else {
                    Snackbar.make(coordinatorLayout, it.message.toString(), Snackbar.LENGTH_LONG)
                }
                it.action?.let { action ->
                    snackbar.setAction(R.string.retry) { action() }
                }
                snackbar.show()
            }
        }

        // back - button logic
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.user.value != null) {
                    when (navController.currentDestination?.id) {
                        R.id.nav_home, R.id.nav_attacks, R.id.nav_abilities, R.id.nav_teams -> finish()
                        else -> navController.navigateUp()
                    }
                }
            }
        })
    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    // overflow-menu functions
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_logout -> {
                viewModel.logout()
                return true
            }
        }
        return false
    }


    /** dialog to update profile pic */
    private fun askIfUserWantsToUpdatePicture() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.change_profile_picture))
            .setPositiveButton(getString(R.string.yes_update)) { _, _ ->
                getContent.launch("image/*") // start intent and let user choose a picture
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }


    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                sharedViewModel.postMessage(R.string.image_is_uploading)
                viewModel.uploadProfilePicture(uri) {
                    updateProfilePicture(it)
                }
            }
        }


    /** Updates the image in the drawer layout */
    private fun updateProfilePicture(photoUrl: Uri?) {
        val navView = binding.navView
        val headerView = navView.getHeaderView(0)
        val imageView: ImageView = headerView.findViewById(R.id.ivProfilePic)
        imageView.load(photoUrl) {
            error(R.drawable.ic_launcher_foreground)
            transformations(CircleCropTransformation())
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /** Restores old drawer navigation logic (if in team builder the navigation logic is modified */
    private fun restoreDrawerNavigation(navView: NavigationView) {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        findViewById<MaterialToolbar>(R.id.mainToolbar).setupWithNavController(
            navController,
            appBarConfiguration
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
}