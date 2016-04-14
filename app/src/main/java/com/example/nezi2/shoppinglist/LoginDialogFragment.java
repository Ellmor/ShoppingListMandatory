package com.example.nezi2.shoppinglist;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;


public class LoginDialogFragment extends DialogFragment {

    public EditText login;
    public EditText pass;

    public EditText getPassView() {
        return pass;
    }

    public EditText getLoginView() {
        return login;
    }

    public static enum LoginResult {SUCCESS, FAILURE}

    private OnLoginDialogListener mListener;

    public LoginDialogFragment() {
        // Required empty public constructor
    }

    public static LoginDialogFragment newInstance() {
        LoginDialogFragment fragment = new LoginDialogFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View view = inflater.inflate(R.layout.dialog_signin, null);
        builder.setView(view)
                .setIcon(R.drawable.ic_menu_gallery)
                .setTitle("Login")
                .setPositiveButton(R.string.signin,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Later in onStart overridden so that the dialog
                                // doesn't close on failed login attempt
                                // this is left for older android versions
                            }
                        }
                )
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mListener.onDialogNegativeClick(LoginDialogFragment.this);
                            }
                        }
                );
        login = (EditText) view.findViewById(R.id.logindialog_username);
        pass = (EditText) view.findViewById(R.id.logindialog_password);
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            Button positiveButton = (Button) d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Do stuff, possibly set wantToCloseDialog to true then...
                    if (mListener.onIsLoginModelValid(LoginDialogFragment.this)) {
                        mListener.onDialogPositiveClick(LoginDialogFragment.this);
                        dismiss();
                    }
                }
            });
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnLoginDialogListener) {
            mListener = (OnLoginDialogListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnLoginDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);

        public void onDialogNegativeClick(DialogFragment dialog);

        public boolean onIsLoginModelValid(DialogFragment dialog);
    }
}
