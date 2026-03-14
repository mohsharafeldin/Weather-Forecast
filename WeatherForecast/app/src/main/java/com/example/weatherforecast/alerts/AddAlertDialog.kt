package com.example.weatherforecast.alerts

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weatherforecast.R
import com.example.weatherforecast.utils.localizeDigits
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddAlertDialog(
    onDismiss: () -> Unit,
    onConfirm: (startTime: Long, endTime: Long, alertType: String, snoozeDuration: Int) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var startTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var endTime by remember { mutableStateOf(System.currentTimeMillis() + 24 * 60 * 60 * 1000) }
    var alertType by remember { mutableStateOf("NOTIFICATION") }
    var snoozeDuration by remember { mutableStateOf(5) }

    val isStartTimeValid = startTime >= System.currentTimeMillis() - 60000 
    val isEndTimeValid = endTime > startTime
    val isValid = isStartTimeValid && isEndTimeValid

    val errorMessage = when {
        !isStartTimeValid -> context.getString(R.string.error_start_time_past)
        !isEndTimeValid -> context.getString(R.string.error_end_time_before_start)
        else -> ""
    }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.add_alert), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.start_time), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = startTime }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        cal.set(year, month, day, hour, minute)
                                        startTime = cal.timeInMillis
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(dateFormat.format(Date(startTime)).localizeDigits())
                }

                Text(stringResource(R.string.end_time), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = endTime }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        cal.set(year, month, day, hour, minute)
                                        endTime = cal.timeInMillis
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(dateFormat.format(Date(endTime)).localizeDigits())
                }

                Text(stringResource(R.string.alert_type), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = alertType == "NOTIFICATION",
                        onClick = { alertType = "NOTIFICATION" }
                    )
                    Text(stringResource(R.string.notification_option), modifier = Modifier.padding(end = 16.dp))
                    RadioButton(
                        selected = alertType == "ALARM",
                        onClick = { alertType = "ALARM" }
                    )
                    Text(stringResource(R.string.alarm_option))
                }

                Text(stringResource(R.string.snooze_duration), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(5, 10, 15).forEach { duration ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = snoozeDuration == duration,
                                onClick = { snoozeDuration = duration }
                            )
                            Text(
                                text = stringResource(R.string.duration_min, duration).localizeDigits(),
                                fontSize = 14.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (!isValid) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(startTime, endTime, alertType, snoozeDuration) },
                enabled = isValid
            ) {
                Text(stringResource(R.string.add_alert))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
