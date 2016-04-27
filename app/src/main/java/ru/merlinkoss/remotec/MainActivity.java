package ru.merlinkoss.remotec;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Spinner;

public class MainActivity extends AppCompatActivity {
    public static String server_name = "http://g93124af.bget.ru";
    // Наше имя сервера


    Spinner spinner_author, spinner_client; // Списки
    String author, client; // Их имена
    Button OpenChat_btn, OpenChatReverce_btn, DeleteServerChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Log.i("chat", "StartMainActivity"); // Для дебага

        OpenChat_btn = (Button) findViewById(R.id.open_chat_btn);
        OpenChatReverce_btn = (Button) findViewById(R.id.open_chat_reverce_btn);
        DeleteServerChat = (Button) findViewById(R.id.delete_server_chat);
        // Обозначили кнопки


        this.startService(new Intent(this, FoneService.class)); // запускаем фон сервис
    }
}
