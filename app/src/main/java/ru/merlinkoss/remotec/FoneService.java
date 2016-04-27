package ru.merlinkoss.remotec;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Admin on 28.04.2016.
 */
public class FoneService extends Service {

    String server_name = MainActivity.server_name; // Возьмем из mainactiv наш сервер

    SQLiteDatabase chatDBlocal;
    HttpURLConnection conn;
    Cursor cursor;
    Thread thread;
    ContentValues new_mess;
    Long last_time; // Время последней записи. Определяем по нем что есть уже


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void onStart(Intent intent, int startId)
    {
        Log.i("chat", " + FoneServiceStart");
        chatDBlocal = openOrCreateDatabase("chatDBlocal.db", Context.MODE_PRIVATE, null);
        chatDBlocal
                .execSQL("CREATE TABLE IF NOT EXISTS chat (_id integer primary key autoincrement, author, client, data, text)");
        // создадим и покажем notification
        // это позволит стать сервису "бессмертным"
        // и будет визуально видно в трее

        Intent iN = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pI = PendingIntent.getActivities(getApplicationContext(),0, new Intent[]{iN}, PendingIntent.FLAG_CANCEL_CURRENT);
        Notification.Builder bI = new Notification.Builder(getApplicationContext()); // Штучка сверху экрана

        bI.setContentIntent(pI)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(),
                        R.mipmap.ic_launcher))
                .setAutoCancel(true)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("А я работаю..."); // Делаем нотификацию вверху

        Notification notification = bI.build();  // СТроим ее
        startForeground(101, notification); // Работает все время пока включено приложение

        startLoop();

    }


    // запуск потока, внутри которого будет происходить
    // регулярное соединение с сервером для чтения новых
    // сообщений.
    // если сообщения найдены - отправим броадкаст для обновления
    // ListView в ChatActivity

    private void startLoop()
    {

        thread = new Thread(new Runnable() {

            // ansver = ответ на запрос
            // lnk = линк с параметрами
            String answer, link;
            @Override
            public void run() {
                while(true)
                {
                    cursor = chatDBlocal.rawQuery("SELECT * FROM chat ORDER BYdata", null);
                    // Запрос к бд по новым сообщениям

                    if(cursor.moveToLast())
                    {
                        last_time = cursor.getLong(cursor.getColumnIndex("data"));
                        link = server_name + "/chat.php?action=select&data=" + last_time.toString();
                        // Если есть новые то формируем запрос по которому получим их
                    }
                    else // А если нет то будем грузить все!
                    {
                        link = server_name + "/chat.php?action=select";
                    }

                    cursor.close();

                    try // Кннектимся к серверу
                    {
                        Log.i("chat", "+ FoneService - СОЕДИНЯЯЯЯЯЕМСЯ");

                        conn = (HttpURLConnection) new URL(link)
                                .openConnection();
                        conn.setReadTimeout(10000);
                        conn.setConnectTimeout(15000);
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                        conn.setDoInput(true);
                        conn.connect();

                    }
                    catch (Exception e) {
                        Log.i("chat", "+ FoneService ошибка: " + e.getMessage());
                    }

                    try //Получаем ответ
                    {
                        InputStream is = conn.getInputStream();
                        BufferedReader bt = new BufferedReader(new InputStreamReader(is,"UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String str = null;
                        while((str = bt.readLine())!= null)
                        {
                            sb.append(str);
                        }

                        Log.i("chat", "+ FoneService - полный ответ сервера:\n"
                                + sb.toString());
                        // сформируем ответ сервера в string
                        // обрежем в полученном ответе все, что находится за "]"
                        // это необходимо, т.к. json ответ приходит с мусором
                        // и если этот мусор не убрать - будет невалидным
                        answer = sb.toString();
                        answer = answer.substring(0,answer.indexOf("]") + 1);

                        is.close();
                        bt.close();
                    }
                    catch (Exception e) {
                        Log.i("chat", "+ FoneService ошибка: " + e.getMessage());
                    } finally {
                        conn.disconnect();
                        Log.i("chat",
                                "+ FoneService --------------- ЗАКРОЕМ СОЕДИНЕНИЕ");
                    }

                    if(answer != null && !answer.trim().equals("")) // Запись ответа в бд
                    {
                        Log.i("chat",
                                "+ FoneService ---------- ответ содержит JSON:");

                        try
                        {
                            //Ответ в JSON массив
                            JSONArray ja = new JSONArray(answer);
                            JSONObject jo;

                            Integer i = 0;

                            while (i < ja.length())
                            {
                                // Разобьем построчно

                                jo = ja.getJSONObject(i);

                                Log.i("chat",
                                        "=================>>> "
                                                + jo.getString("author")
                                                + " | "
                                                + jo.getString("client")
                                                + " | " + jo.getLong("data")
                                                + " | " + jo.getString("text"));

                                // Создадим новое сообщение
                                new_mess = new ContentValues();
                                new_mess.put("author", jo.getString("author"));
                                new_mess.put("client", jo.getString("client"));
                                new_mess.put("data", jo.getLong("data"));
                                new_mess.put("text", jo.getString("text"));
                                chatDBlocal.insert("chat", null, new_mess);// Запишем в бд
                                new_mess.clear();

                                i++;

                                // отправим броадкаст для ChatActivity
                                // если она открыта - она обновить ListView

                                sendBroadcast(new Intent("ru.merlinkoss.action.UPDATE_ListView"));
                            }


                        } catch (Exception e) {
                            // если ответ сервера не содержит валидный JSON
                            Log.i("chat",
                                    "+ FoneService ---------- ошибка ответа сервера:\n"
                                            + e.getMessage());
                        }


                    } else {
                        // если ответ сервера пустой
                        Log.i("chat",
                                "+ FoneService ---------- ответ не содержит JSON!");
                    }
                    try {
                        Thread.sleep(15000);
                    } catch (Exception e) {
                        Log.i("chat",
                                "+ FoneService - ошибка процесса: "
                                        + e.getMessage());
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
