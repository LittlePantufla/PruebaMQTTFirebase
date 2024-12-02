package com.stomas.appmqtt;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.Spinner;
//Firebase
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    // Se declaran la variables de firebase
    private EditText txtCodigo, txtmensaje;
    private ListView lista;
    private FirebaseFirestore db;

    //Variables de nuestra conexion a la MQTT
    private static String mqttHost = "tcp://venombow894:9RnlMxlctiYrzbWq@venombow894.cloud.shiftr.io:1883"; //IP del servidor
    private static String IdUsuario = "AppAndroid";

    private static String Topico = "mensaje";
    private static String User = "venombow894";
    private static String Pass = "9RnlMxlctiYrzbWq";

    private TextView textView;
    private EditText editTextMessage;
    private Button botonEnvio;

    //Librearia MQTT
    private MqttClient mqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //LLamamos al metodo list
        CargarListaFirestore();
        //Se inicia la firestore
        db = FirebaseFirestore.getInstance();
        //Se unen las varibales del XML
        txtmensaje = findViewById(R.id.txtmensaje);
        txtCodigo = findViewById(R.id.txtCodigo);
        lista = findViewById(R.id.lista);

        textView = findViewById(R.id.textView);
        editTextMessage = findViewById(R.id.txtmensaje);
        botonEnvio = findViewById(R.id.botonEnvioMensaje);

        try {
            mqttClient = new MqttClient(mqttHost, IdUsuario, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(User);
            options.setPassword(Pass.toCharArray());
            //Conexion con el mqtt
            mqttClient.connect(options);
            //En el caso de conectarse de imprime
            Toast.makeText(this, "Aplicacion conectada al Servidor MQTT", Toast.LENGTH_SHORT).show();
            //Manejo de entrega de datos y la perdida de la conexion
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.d("MQTTT", "Conexion Perdida");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    runOnUiThread(() -> textView.setText(payload));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("MQTT", "Entrega Completa");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
        //El boton enviara un mensaje de topico
        botonEnvio.setOnClickListener(new View.OnClickListener() {
            @Override
                    public void onClick(View view) {
                // Obtener el mensaje ingresado por el usuario
                String mensaje = editTextMessage.getText().toString();
                try {
                    //Verificacion de conexion
                    if (mqttClient != null && mqttClient.isConnected()) {
                        mqttClient.publish(Topico, mensaje.getBytes(), 0, false);
                        textView.append("\n - "+ mensaje);
                        Toast.makeText(MainActivity.this, "Mensaje enviado", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Error: No se pudo enviar el mensaje. La conexion MQTT no esta activa",Toast.LENGTH_SHORT).show();
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    //metodo para enviar los datos
    public void enviarDatosFirestore(View view){
        //Se obtienen los campos ingresado en el formulari
        String codigo = txtCodigo.getText().toString();
        String mensaje = txtmensaje.getText().toString();

        //Se crea un mapa con los datos a enviar
        Map<String, Object> opinion = new HashMap<>();
        opinion.put("codigo", codigo);
        opinion.put("mensaje", mensaje);

        //Se envian los datos a la base de datos
        db.collection("opinion")
                .document(codigo)
                .set(opinion)
                .addOnSuccessListener(aVoid -> {
            Toast.makeText(MainActivity.this, "Datos Enviados a Firestore correctamente", Toast.LENGTH_SHORT).show();
        })
        .addOnFailureListener(e -> {
            Toast.makeText(MainActivity.this, "Error al enviar datos al FireStore" + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
    public void CargarLista(View view) {
        CargarListaFirestore();
    }
    //Crear el metodo lista
    public void CargarListaFirestore(){
        //Aqui va el codigo para cargar desde Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        //Se hace una consulta
        db.collection("opinion")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            //Si la consulta es exitosa, se procesan los documentos obetenido
                            List<String> listaOpinion = new ArrayList<>();
                            //Recorre lso datos obtenidos
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String linea = "|| " +
                                        document.getString("codigo") + " || " +
                                        document.getString("mensaje") + " || ";
                                listaOpinion.add(linea);
                            }
                            //Crea un ArrayAdapter
                            ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                                    MainActivity.this,
                                    android.R.layout.simple_list_item_1,
                                    listaOpinion
                            );
                            lista.setAdapter(adaptador);
                        } else {
                            Log.e("TAG", "Error al obtener datos de Firestore", task.getException());
                        }
                    }
                });
    }
}