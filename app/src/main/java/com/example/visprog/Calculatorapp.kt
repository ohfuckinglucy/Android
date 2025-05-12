package com.example.visprog


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import com.example.visprog.ui.theme.Dblue
import com.example.visprog.ui.theme.Lblue

//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            CalculatorApp()
//        }
//    }
//}

@Composable
fun CalculatorApp() {
    var input1 by remember { mutableStateOf("0") }
    var input2 by remember { mutableStateOf("0") }
    var operation by remember { mutableStateOf("+") }
    var result by remember { mutableIntStateOf(0) }
    var previousExpression by remember { mutableStateOf("") }
    var isSecondInput by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFB0DAFF))
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (previousExpression.isNotEmpty()) {
            Text(
                text = previousExpression,
                fontSize = 24.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Text(
            text = "$input1 $operation $input2",
            fontSize = 28.sp,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(8.dp)
        )

        Box(
            modifier = Modifier
                .size(150.dp)
                .pointerInput(Unit) {
                    detectGestures(
                        onSwipeUp = {
                            operation = "+"
                            isSecondInput = true
                        },
                        onSwipeDown = {
                            operation = "-"
                            isSecondInput = true
                        },
                        onSwipeRUp = {
                            operation = "*"
                            isSecondInput = true
                        },
                        onSwipeLUp =  {
                            operation = "^"
                            isSecondInput = true
                        },
                        onSwipeRDown =  {
                            operation = "/"
                            isSecondInput = true
                        },
                        onSwipeLDown =  {
                            operation = "%"
                            isSecondInput = true
                        },
                        onSwipeLeft = {
                            previousExpression = ""
                            input1 = "0"
                            input2 = "0"
                            result = 0
                            isSecondInput = false
                        },
                        onSwipeRight = {
                            val calcResult = calculateResult(input1, input2, operation)
                            previousExpression = "$input1 $operation $input2 = $calcResult"
                            result = calcResult
                            isSecondInput = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                TextField(
                    value = input1,
                    onValueChange = { input1 = it},
                    label = {Text("num 1")},
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Lblue,
                        focusedContainerColor = Dblue,
                        unfocusedTextColor = Color.Black,
                        focusedTextColor = Color.Black

                    )

                )

                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = input2,
                    onValueChange = { input2 = it},
                    label = {Text("num 2")},
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Lblue,
                        focusedContainerColor = Dblue,
                        unfocusedTextColor = Color.Black,
                        focusedTextColor = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

fun calculateResult(input1: String, input2: String, operation: String): Int {
    val num1 = input1.toIntOrNull()
    val num2 = input2.toIntOrNull()

    if (num1 == null || num2 == null){
        return 0
    }

    return when (operation) {
        "+" -> num1 + num2
        "-" -> num1 - num2
        "*" -> num1 * num2
        "^" -> num1.toDouble().pow(num2.toDouble()).toInt()
        "/" -> if (num2 != 0) num1 / num2 else 0
        "%" -> if (num2 != 0) num1 % num2 else 0
        else -> 0
    }
}

suspend fun PointerInputScope.detectGestures(
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeRUp: () -> Unit,
    onSwipeLUp: () -> Unit,
    onSwipeRDown: () -> Unit,
    onSwipeLDown: () -> Unit
) {
    awaitPointerEventScope {
        while (true) {
            val down = awaitPointerEvent().changes.first().position
            val event = awaitPointerEvent()
            val up = event.changes.first().position

            val dx = up.x - down.x
            val dy = up.y - down.y
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)

            if (distance < 50f) continue

            val angle = kotlin.math.atan2(-dy, dx) * (180 / Math.PI)

            when (angle) {
                in -30.0..30.0 -> onSwipeRight()
                in 150.0..180.0, in -180.0..-150.0 -> onSwipeLeft()
                in 60.0..120.0 -> onSwipeUp()
                in -120.0..-60.0 -> onSwipeDown()
                in 30.0..60.0 -> onSwipeRUp()
                in 120.0..150.0 -> onSwipeLUp()
                in -60.0..-30.0 -> onSwipeRDown()
                in -150.0..-120.0 -> onSwipeLDown()
            }
        }
    }
}