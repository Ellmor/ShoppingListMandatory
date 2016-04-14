package com.example.nezi2.shoppinglist;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.Map;

import model.ShoppingItem;
import service.Service;
import service.ShoppingListArrayAdapter;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, LoginDialogFragment.OnLoginDialogListener {

    private static final String TAG = "ShoppingList";
    //Random
    private CoordinatorLayout coordinatorLayout;
    ListView listView;
    private ShoppingListArrayAdapter<ShoppingItem> adapter;

    //Auth

    //Nav Login Button
    private View nav_login_btn;

    //Nav Register Button
    private View nav_register_btn;

    //Nav Logout Button
    private View nav_logout_btn;

    /* A dialog that is presented until the Firebase authentication finished. */
    private ProgressDialog mAuthProgressDialog;

    /* Data from the authenticated user */
    private AuthData mAuthData;

    /* Listener for Firebase session changes */
    private Firebase.AuthStateListener mAuthStateListener;

    /* *************************************
    *              PASSWORD               *
    ***************************************/
    private Button mPasswordLoginButton;

    /* *************************************
     *            ANONYMOUSLY              *
     ***************************************/
    private Button mAnonymousLoginButton;
    //Service
    private Service Service = service.Service.getInstance();
    private Firebase FireBaseRef;
    private TextView mLoggedInStatusTextView;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Action bar
        //getActionBar().setHomeButtonEnabled(true); //this means we can click "home"

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id
                .coordinatorLayout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.btnAdd);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("Button Add onClick");
            }
        });

        /////////
        // LIST
        /////////

        listView = (ListView) findViewById(R.id.list);
        adapter = new ShoppingListArrayAdapter<ShoppingItem>(this,
                R.layout.listviewitemlayout, Service.getItems());
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Firebase
        //
        Firebase.setAndroidContext(this);

        /* *************************************
         *               GENERAL               *
         ***************************************/
        mLoggedInStatusTextView = (TextView) findViewById(R.id.login_status);

        /* Create the Firebase ref that is used for all authentication with Firebase */
        FireBaseRef = new Firebase(getResources().getString(R.string.firebase_url));

        /* Setup the progress dialog that is displayed later when authenticating with Firebase */
        mAuthProgressDialog = new ProgressDialog(this);
        mAuthProgressDialog.setTitle("Loading");
        mAuthProgressDialog.setMessage("Authenticating with Firebase...");
        mAuthProgressDialog.setCancelable(false);
        mAuthProgressDialog.show();

        mAuthStateListener = new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                mAuthProgressDialog.hide();
                setAuthenticatedUser(authData);
            }
        };
        /* Check if the user is authenticated with Firebase already. If this is the case we can set the authenticated
         * user and hide hide any login buttons */
        FireBaseRef.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // if changing configurations, stop tracking firebase session.
        FireBaseRef.removeAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onStart() {
        super.onStart();

        /* Create the Firebase ref that is used for all authentication with Firebase */
        FireBaseRef = new Firebase(getResources().getString(R.string.firebase_url));
        FireBaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String newCondition = (String) dataSnapshot.getValue();

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
         /* If a user is currently authenticated, display a logout menu
          * We want to display menu anyways for other options than logout */
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.item_logout).setVisible(this.mAuthData != null);
//        if (this.mAuthData != null) {
//            return true; //show menu
//        } else {
//            return false;// don't show menu
//        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Toast.makeText(this, "Settings icon clicked!",
                        Toast.LENGTH_SHORT).show();
                return true;
            case android.R.id.home:
                Toast.makeText(this, "Application icon clicked!",
                        Toast.LENGTH_SHORT).show();
                return true; //return true, means we have handled the event
            case R.id.item_about:
                Toast.makeText(this, "About item clicked!", Toast.LENGTH_SHORT)
                        .show();
                return true;
            case R.id.item_delete:
                Toast.makeText(this, "Delete item clicked!", Toast.LENGTH_SHORT)
                        .show();
                if (Service.getItems().size() > 0) {
                    new AlertDialog.Builder(this)
                            .setMessage("Do you really want to delete all items?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    //Toast.makeText(MainActivity.this, "Yaay", Toast.LENGTH_SHORT).show();
                                    Snackbar snackbar = Snackbar
                                            .make(coordinatorLayout, "Welcome to AndroidHive", Snackbar.LENGTH_LONG);
                                    snackbar.show();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();
                } else {
                    //If there are not items to be removed
                    //Do nothing
                }
                return true;
            case R.id.item_refresh:
                Toast.makeText(this, "Refresh item clicked!", Toast.LENGTH_SHORT)
                        .show();
                return true;
            case R.id.item_logout:
                logout();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_login) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            showDia();
            return true;
        } else if (id == R.id.nav_logout) {
            logout();
            return true;
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    void showDia() {
        DialogFragment newFragment = LoginDialogFragment.newInstance();
        newFragment.show(getSupportFragmentManager(), "dialog");
    }

    ////////////////////////////////////////////////////////////////
    //
    //                      LOGIN DIALOG
    //
    ////////////////////////////////////////////////////////////////

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        //check if password inserted
        loginWithPassword();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        System.out.println("DialogFragment: Negative Click");
    }

    @Override
    public boolean onIsLoginModelValid(DialogFragment dialog) {
        EditText pass = ((LoginDialogFragment) dialog).getPassView();
        EditText login = ((LoginDialogFragment) dialog).getLoginView();
        String strPass = pass.toString();
        String strLogin = login.toString();
        //login.getText().toString()!="" &&
        if (strLogin.length() >= 1){
            if (strPass != null && strPass.length() > 1) {
                return true;
            }else{
                login.setError("Password cannot be empty.");
            }
        }else{
            login.setError("Login must be at least 1 character long.");
        }
        return false;
    }

    //This will be called when other activities in our application
    //are finished.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) //exited our preference screen
        {
            Toast toast =
                    Toast.makeText(getApplicationContext(), "back from preferences", Toast.LENGTH_LONG);
            toast.setText("back from our preferences");
            toast.show();
            //here you could put code to do something.......
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setPreferences(View v) {
        //Here we create a new activity and we instruct the
        //Android system to start it
        Intent intent = new Intent(this, SettingsActivity.class);
        //startActivity(intent); //this we can use if we DONT CARE ABOUT RESULT

        //we can use this, if we need to know when the user exists our preference screens
        startActivityForResult(intent, 1);
    }

    public void getPreferences(View v) {

        //We read the shared preferences from the
        SharedPreferences prefs = getSharedPreferences("my_prefs", MODE_PRIVATE);
        String email = prefs.getString("email", "");
        String gender = prefs.getString("gender", "");
        boolean soundEnabled = prefs.getBoolean("sound", false);

        Toast.makeText(
                this,
                "Email: " + email + "\nGender: " + gender + "\nSound Enabled: "
                        + soundEnabled, Toast.LENGTH_SHORT).show();
    }

    /**
     * Unauthenticate from Firebase and from providers where necessary.
     */
    private void logout() {
        if (this.mAuthData != null) {
            /* logout of Firebase */
            FireBaseRef.unauth();
            /* Logout of any of the Frameworks. This step is optional, but ensures the user is not logged into
             * Facebook/Google+ after logging out of Firebase. */
//            if (this.mAuthData.getProvider().equals("facebook")) {
//                /* Logout from Facebook */
//                LoginManager.getInstance().logOut();
//            } else if (this.mAuthData.getProvider().equals("google")) {
//                /* Logout from Google+ */
//                if (mGoogleApiClient.isConnected()) {
//                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
//                    mGoogleApiClient.disconnect();
//                }
//            }
            /* Update authenticated user and show login buttons */
            setAuthenticatedUser(null);
        }
    }

    /**
     * This method will attempt to authenticate a user to firebase given an oauth_token (and other
     * necessary parameters depending on the provider)
     */
    private void authWithFirebase(final String provider, Map<String, String> options) {
        if (options.containsKey("error")) {
            showErrorDialog(options.get("error"));
        } else {
            mAuthProgressDialog.show();
            if (provider.equals("twitter")) {
                // if the provider is twitter, we pust pass in additional options, so use the options endpoint
                FireBaseRef.authWithOAuthToken(provider, options, new AuthResultHandler(provider));
            } else {
                // if the provider is not twitter, we just need to pass in the oauth_token
                FireBaseRef.authWithOAuthToken(provider, options.get("oauth_token"), new AuthResultHandler(provider));
            }
        }
    }

    /**
     * Once a user is logged in, take the mAuthData provided from Firebase and "use" it.
     */
    private void setAuthenticatedUser(AuthData authData) {
        if (authData != null) {
            /* Hide all the login buttons */
            //mFacebookLoginButton.setVisibility(View.GONE);
            //mGoogleLoginButton.setVisibility(View.GONE);
            //mTwitterLoginButton.setVisibility(View.GONE);
            //mPasswordLoginButton.setVisibility(View.GONE);
            //mAnonymousLoginButton.setVisibility(View.GONE);
            //navigationView.getMenu().setGroupVisible(R.id.loginGroup, false);
            //navigationView.getMenu().setGroupVisible(R.id.logoutGroup, true);

            /* show a provider specific status text */
            mLoggedInStatusTextView.setVisibility(View.VISIBLE);
            String name = null;
            if (authData.getProvider().equals("facebook")
                    || authData.getProvider().equals("google")
                    || authData.getProvider().equals("twitter")) {
                name = (String) authData.getProviderData().get("displayName");
            } else if (authData.getProvider().equals("anonymous")
                    || authData.getProvider().equals("password")) {
                name = authData.getUid();
            } else {
                Log.e(TAG, "Invalid provider: " + authData.getProvider());
            }
            if (name != null) {
                mLoggedInStatusTextView.setText("Logged in as " + name + " (" + authData.getProvider() + ")");
                Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
            }
        } else {
            /* No authenticated user show all the login buttons */
//            mFacebookLoginButton.setVisibility(View.VISIBLE);
//            mGoogleLoginButton.setVisibility(View.VISIBLE);
//            mTwitterLoginButton.setVisibility(View.VISIBLE);
//            mPasswordLoginButton.setVisibility(View.VISIBLE);
//            mAnonymousLoginButton.setVisibility(View.VISIBLE);
            mLoggedInStatusTextView.setVisibility(View.GONE);
            //navigationView.getMenu().setGroupVisible(R.id.loginGroup, true);
            //navigationView.getMenu().setGroupVisible(R.id.logoutGroup, false);
        }
        this.mAuthData = authData;
        /* invalidate options menu to hide/show the logout button */
        supportInvalidateOptionsMenu();
    }

    /**
     * Show errors to users
     */
    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Utility class for authentication results
     */
    private class AuthResultHandler implements Firebase.AuthResultHandler {

        private final String provider;

        public AuthResultHandler(String provider) {
            this.provider = provider;
        }

        @Override
        public void onAuthenticated(AuthData authData) {
            mAuthProgressDialog.hide();
            Log.i(TAG, provider + " auth successful");
            setAuthenticatedUser(authData);
        }

        @Override
        public void onAuthenticationError(FirebaseError firebaseError) {
            mAuthProgressDialog.hide();
            showErrorDialog(firebaseError.toString());
        }
    }

    /* ************************************
     *              PASSWORD              *
     **************************************
     */
    public void loginWithPassword() {
        mAuthProgressDialog.show();
        FireBaseRef.authWithPassword("susu@o2.pl", "nezi12", new AuthResultHandler("password"));
    }

}
