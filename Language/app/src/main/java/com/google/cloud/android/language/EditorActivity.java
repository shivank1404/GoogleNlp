package com.google.cloud.android.language;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.susmit.aceeditor.AceEditor;
import com.susmit.aceeditor.OnLoadedEditorListener;
import com.susmit.aceeditor.ResultReceivedListener;

public class EditorActivity extends Activity {

    FloatingActionButton analyze;
    AceEditor editor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actuvity_main_new);

        editor = findViewById(R.id.editor);

        editor.setOnLoadedEditorListener(new OnLoadedEditorListener() {
            @Override
            public void onCreate() {
                editor.setText(getResources().getString(R.string.sample_text));
            }
        });
        editor.setResultReceivedListener(new ResultReceivedListener() {
            @Override
            public void onReceived(int FLAG_VALUE, String... results) {
                if(FLAG_VALUE==AceEditor.Request.TEXT_REQUEST){
                    Intent i = new Intent(EditorActivity.this, MainActivity.class);
                    i.putExtra("text",results[0]);
                    startActivity(i);
                }
            }
        });

        analyze = findViewById(R.id.analyze);

        analyze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.requestText();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        analyze.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        analyze.setVisibility(View.VISIBLE);
    }
}
