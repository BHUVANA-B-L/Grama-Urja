package com.example.gramaurja

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.google.firebase.database.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1
            )
        }

        setContent {
            GramaUrjaApp(this)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "power_channel",
                "Power Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

//////////////////// APP ////////////////////

@Composable
fun GramaUrjaApp(context: Context) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "zone") {
        composable("zone") { ZoneScreen(navController) }
        composable("home/{zone}") {
            val zone = it.arguments?.getString("zone") ?: "zone1"
            HomeScreen(navController, zone, context)
        }
        composable("pump") { PumpScreen(navController) }
    }
}

//////////////////// ZONE ////////////////////

//////////////////// ZONE ////////////////////

@Composable
fun ZoneScreen(navController: NavController) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF6A5ACD),
                        Color(0xFF00C9A7)
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),

            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "⚡ GRAMA URJA",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Smart Power for Smart Farming",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(0.85f)
            )

            Spacer(modifier = Modifier.height(34.dp))

            Text(
                text = "Select Your Zone",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ZONE 1

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),

                shape = RoundedCornerShape(24.dp),

                elevation = CardDefaults.cardElevation(
                    defaultElevation = 10.dp
                ),

                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),

                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {

                        Text(
                            "Zone 1",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text(
                            "Transformer A",
                            color = Color.Gray
                        )
                    }

                    Button(
                        shape = RoundedCornerShape(14.dp),

                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6A5ACD)
                        ),

                        onClick = {
                            navController.navigate("home/zone1")
                        }
                    ) {
                        Text("Select")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ZONE 2

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),

                shape = RoundedCornerShape(24.dp),

                elevation = CardDefaults.cardElevation(
                    defaultElevation = 10.dp
                ),

                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),

                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {

                        Text(
                            "Zone 2",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text(
                            "Transformer B",
                            color = Color.Gray
                        )
                    }

                    Button(
                        shape = RoundedCornerShape(14.dp),

                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6A5ACD)
                        ),

                        onClick = {
                            navController.navigate("home/zone2")
                        }
                    ) {
                        Text("Select")
                    }
                }
            }
        }
    }
}

//////////////////// HOME ////////////////////

@Composable
fun HomeScreen(navController: NavController, selectedZone: String, context: Context) {

    var status by remember { mutableStateOf("OFF") }
    var lastSeen by remember { mutableStateOf("--") }

    val database = FirebaseDatabase.getInstance(
        "https://gramaurja-1e911-default-rtdb.asia-southeast1.firebasedatabase.app/"
    )

    val zoneRef = remember(selectedZone) {
        database.getReference("zones").child(selectedZone)
    }

    var lastNotifiedStatus by remember { mutableStateOf<String?>(null) }

    DisposableEffect(selectedZone) {

        val listener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val newStatus = snapshot.child("powerStatus")
                    .getValue(String::class.java) ?: "OFF"

                val timeValue = snapshot.child("timestamp")
                    .getValue(Long::class.java)

                if (lastNotifiedStatus != null &&
                    newStatus != lastNotifiedStatus
                ) {
                    sendNotification(
                        context,
                        "Power $newStatus in $selectedZone"
                    )
                }

                lastNotifiedStatus = newStatus
                status = newStatus

                lastSeen =
                    if (timeValue != null)
                        getTimeAgo(timeValue)
                    else "--"
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        zoneRef.addValueEventListener(listener)

        onDispose {
            zoneRef.removeEventListener(listener)
        }
    }

    val statusColor =
        if (status == "ON")
            Color(0xFF00C853)
        else
            Color(0xFFD50000)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF4A00E0),
                        Color(0xFF00C9A7)
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {

            // HEADER

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "⚡ GRAMA URJA",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )

            Text(
                "Smart Power Monitoring",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(0.85f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(50.dp),
                color = Color.White.copy(0.15f)
            ) {
                Text(
                    text = "📍 Zone: $selectedZone",
                    color = Color.White,
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // STATUS CARD

            Card(
                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(30.dp),

                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),

                elevation = CardDefaults.cardElevation(
                    defaultElevation = 12.dp
                )
            ) {

                Column(
                    modifier = Modifier.padding(26.dp)
                ) {

                    Text(
                        if (status == "ON") "⚡"
                        else "❌",

                        style = MaterialTheme.typography.displayLarge
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        if (status == "ON")
                            "POWER AVAILABLE"
                        else
                            "POWER NOT AVAILABLE",

                        style = MaterialTheme.typography.headlineMedium,
                        color = statusColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        if (status == "ON")
                            "Electricity is currently available"
                        else
                            "Electricity supply is currently unavailable",

                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFE8F5E9)
                    ) {

                        Text(
                            "🕒 Last Updated: $lastSeen",

                            modifier = Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 10.dp
                            ),

                            color = Color.DarkGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // TURN ON / TURN OFF AT TOP

            Row {

                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),

                    shape = RoundedCornerShape(20.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00C853)
                    ),

                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp
                    ),

                    onClick = {
                        zoneRef.setValue(
                            mapOf(
                                "powerStatus" to "ON",
                                "timestamp" to System.currentTimeMillis()
                            )
                        )
                    }
                ) {

                    Text(
                        "⚡ TURN ON",
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),

                    shape = RoundedCornerShape(20.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD50000)
                    ),

                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp
                    ),

                    onClick = {
                        zoneRef.setValue(
                            mapOf(
                                "powerStatus" to "OFF",
                                "timestamp" to System.currentTimeMillis()
                            )
                        )
                    }
                ) {

                    Text(
                        "❌ TURN OFF",
                        color = Color.White
                    )
                }
            }

            // PUSH BOTTOM BUTTONS DOWN

            Spacer(modifier = Modifier.weight(1f))

            // CHANGE ZONE BOTTOM

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),

                shape = RoundedCornerShape(20.dp),

                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7E57C2)
                ),

                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 10.dp
                ),

                onClick = {
                    navController.navigate("zone") {
                        popUpTo("zone") { inclusive = true }
                    }
                }
            ) {

                Text(
                    "🔄 Change Transformer Zone",
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PUMP TIMER BOTTOM

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),

                shape = RoundedCornerShape(20.dp),

                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1565C0)
                ),

                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 10.dp
                ),

                onClick = {
                    navController.navigate("pump")
                }
            ) {

                Text(
                    "🚰 Open Pump Timer",
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

//////////////////// PUMP ////////////////////

@Composable
fun PumpScreen(navController: NavController) {

    var crop by remember { mutableStateOf("") }
    var time by remember { mutableStateOf(0) }

    var water by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF11998E),
                        Color(0xFF38EF7D)
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {

            Spacer(modifier = Modifier.height(20.dp))

            // HEADER

            Text(
                "🚰 Smart Pump Timer",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )

            Text(
                "Estimate irrigation duration for crops",
                color = Color.White.copy(0.85f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // CROPS TITLE

            Text(
                "Select Crop Type",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(18.dp))

            // CROP BUTTONS

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Button(
                    shape = RoundedCornerShape(18.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6A1B9A)
                    ),

                    onClick = {
                        crop = "Rice"
                        time = 30
                        water = "High Water Usage"
                    }
                ) {
                    Text("🌾 Rice")
                }

                Button(
                    shape = RoundedCornerShape(18.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5E35B1)
                    ),

                    onClick = {
                        crop = "Wheat"
                        time = 20
                        water = "Medium Water Usage"
                    }
                ) {
                    Text("🌱 Wheat")
                }

                Button(
                    shape = RoundedCornerShape(18.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3949AB)
                    ),

                    onClick = {
                        crop = "Sugarcane"
                        time = 45
                        water = "Very High Water Usage"
                    }
                ) {
                    Text("🎋 Sugarcane")
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // RESULT CARD

            Card(
                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(28.dp),

                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),

                elevation = CardDefaults.cardElevation(
                    defaultElevation = 10.dp
                )
            ) {

                Column(
                    modifier = Modifier.padding(24.dp)
                ) {

                    if (crop.isEmpty()) {

                        Text(
                            "Select a crop to view irrigation details",
                            style = MaterialTheme.typography.bodyLarge
                        )

                    } else {

                        Text(
                            "Selected Crop",
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            crop,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFF1B5E20)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            "⏱ Recommended Pump Time",
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            "$time Minutes",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFF1565C0)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            "💧 Water Requirement",
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            water,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFE8F5E9)
                        ) {

                            Text(
                                "✅ Efficient irrigation helps save water and electricity.",

                                modifier = Modifier.padding(14.dp),

                                color = Color(0xFF1B5E20)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // BACK BUTTON

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),

                shape = RoundedCornerShape(20.dp),

                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF004D40)
                ),

                onClick = {
                    navController.popBackStack()
                }
            ) {

                Text(
                    "⬅ Back to Home",
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

//////////////////// NOTIFICATION ////////////////////

fun sendNotification(context: Context, message: String) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
    }

    val builder = NotificationCompat.Builder(context, "power_channel")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Grama Urja Alert")
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    NotificationManagerCompat.from(context)
        .notify(System.currentTimeMillis().toInt(), builder.build())
}

//////////////////// LOGIC ////////////////////

fun calculatePumpTime(crop: String): Int {
    return when (crop) {
        "Rice" -> 30
        "Wheat" -> 20
        "Sugarcane" -> 45
        else -> 25
    }
}

fun getTimeAgo(time: Long): String {
    val diff = System.currentTimeMillis() - time
    val minutes = diff / 1000 / 60
    val hours = minutes / 60

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes mins ago"
        hours < 24 -> "$hours hrs ago"
        else -> "${hours / 24} days ago"
    }
}